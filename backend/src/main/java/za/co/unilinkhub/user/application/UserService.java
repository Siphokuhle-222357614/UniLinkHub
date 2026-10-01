package za.co.unilinkhub.user.application;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import za.co.unilinkhub.audit.application.AuditLogService;
import za.co.unilinkhub.common.exception.BadRequestException;
import za.co.unilinkhub.common.exception.ForbiddenException;
import za.co.unilinkhub.common.exception.ResourceNotFoundException;
import za.co.unilinkhub.common.web.RejectionMessages;
import za.co.unilinkhub.mail.AccountEmails;
import za.co.unilinkhub.user.domain.AccountStatus;
import za.co.unilinkhub.user.domain.User;
import za.co.unilinkhub.user.domain.UserRole;
import za.co.unilinkhub.user.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final AuditLogService auditLogService;
    private final AccountEmails accountEmails;

    public UserDTO getById(UUID id) {
        return UserDTO.from(findUser(id));
    }

    public UserDTO verifyEmail(String token) {
        User user = userRepository.findByVerificationToken(token == null ? "" : token.trim())
                .orElseThrow(() -> new BadRequestException("This verification link isn't valid any more - it may have been used already. "
                        + "If your account still isn't active, request a new link from the login page."));
        user.verifyEmail(token.trim(), LocalDateTime.now());
        return UserDTO.from(userRepository.save(user));
    }

    /**
     * Public "resend verification email". Deliberately silent when the address isn't registered or
     * is already verified, so it can't be used to find out which students have accounts.
     */
    public void resendVerification(String email) {
        userRepository.findByEmail(StudentEmailPolicy.normalize(email))
                .filter(u -> u.getAccountStatus() == AccountStatus.PENDING_VERIFICATION)
                .ifPresent(this::issueVerificationEmail);
    }

    /** Admin console: re-send the link for a student who says they never got it. */
    public UserDTO resendVerificationFor(UUID userId, UUID adminId) {
        User user = findUser(userId);
        if (user.getAccountStatus() != AccountStatus.PENDING_VERIFICATION) {
            throw new BadRequestException(user.getFirstName() + " has already verified their email, so there's nothing to resend.");
        }
        issueVerificationEmail(user);
        auditLogService.record(findUser(adminId).getFullName(), "ACCOUNT",
                "Re-sent the verification email to \"" + user.getFullName() + "\"");
        return UserDTO.from(user);
    }

    private void issueVerificationEmail(User user) {
        String token = UUID.randomUUID().toString();
        user.renewVerificationToken(token, LocalDateTime.now().plus(RegisterUserUseCase.VERIFICATION_LINK_LIFETIME));
        userRepository.save(user);
        accountEmails.sendVerification(user, token);
    }

    /**
     * Unlocks seller tools. The student must have agreed to the marketplace rules (no alcohol,
     * drugs, weapons or other restricted items) - we record when, so it's on file if a rule is broken.
     */
    public UserDTO becomeSeller(UUID userId, boolean acceptedRules) {
        User user = findUser(userId);
        if (user.isAdmin()) {
            throw new ForbiddenException("Admin accounts can't become sellers. Admin accounts are only for running UniLinkHub, "
                    + "so they stay neutral - use a student account to sell.", RejectionMessages.CODE_ADMIN_ACCOUNT);
        }
        if (!acceptedRules && user.getSellerRulesAcceptedAt() == null) {
            throw new BadRequestException("Please read and accept the marketplace rules before you start selling. "
                    + "They explain what can't be sold on UniLinkHub (like alcohol, drugs and weapons) and what happens if someone does.");
        }
        user.becomeSeller(LocalDateTime.now());
        return UserDTO.from(userRepository.save(user));
    }

    public List<UserDTO> listPendingAccounts() {
        return userRepository.findByAccountStatus(AccountStatus.PENDING_VERIFICATION).stream()
                .filter(u -> !u.isAdmin())
                .map(UserDTO::from)
                .toList();
    }

    /** Student accounts only - admin accounts are listed separately (AdminAccountService). */
    public List<UserDTO> listAccounts(String status, String keyword) {
        List<User> users = userRepository.findAll().stream().filter(u -> u.getRole() == UserRole.STUDENT).toList();

        if (status != null && !status.isBlank() && !"ALL".equalsIgnoreCase(status)) {
            AccountStatus filter = parseStatus(status);
            users = users.stream().filter(u -> u.getAccountStatus() == filter).toList();
        }

        if (keyword != null && !keyword.isBlank()) {
            String needle = keyword.toLowerCase();
            users = users.stream()
                    .filter(u -> u.getFullName().toLowerCase().contains(needle)
                            || u.getEmail().toLowerCase().contains(needle)
                            || (u.getStudentNumber() != null && u.getStudentNumber().toLowerCase().contains(needle)))
                    .toList();
        }

        return users.stream()
                .sorted(Comparator.comparing(User::getCreatedAt).reversed())
                .map(UserDTO::from)
                .toList();
    }

    private static AccountStatus parseStatus(String status) {
        try {
            return AccountStatus.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException("\"" + status + "\" isn't an account status we recognise. Use Active, Pending verification, Suspended or Deactivated.");
        }
    }

    public UserDTO suspendAccount(UUID userId, UUID adminId, String reason) {
        User user = findUser(userId);
        if (Objects.equals(userId, adminId)) {
            throw new ForbiddenException("You can't suspend your own account.");
        }
        if (user.isAdmin()) {
            throw new ForbiddenException("Admin accounts can't be suspended from the student accounts list. "
                    + "Suspending is for students who break the marketplace rules.");
        }
        user.suspend(reason);
        UserDTO dto = UserDTO.from(userRepository.save(user));
        auditLogService.record(findUser(adminId).getFullName(), "ACCOUNT", "Suspended account \"" + user.getFullName() + "\""
                + (reason != null && !reason.isBlank() ? " - reason: " + reason : ""));
        return dto;
    }

    public UserDTO reactivateAccount(UUID userId, UUID adminId) {
        User user = findUser(userId);
        if (user.getAccountStatus() == AccountStatus.PENDING_VERIFICATION) {
            throw new BadRequestException(user.getFirstName() + " hasn't verified their email yet. Students activate their own "
                    + "account by clicking the link we email them - you can re-send it instead.");
        }
        user.reactivate();
        UserDTO dto = UserDTO.from(userRepository.save(user));
        auditLogService.record(findUser(adminId).getFullName(), "ACCOUNT", "Reactivated account \"" + user.getFullName() + "\"");
        return dto;
    }

    public UserDTO updateNotificationPreferences(UUID userId, List<String> disabledCategories) {
        User user = findUser(userId);
        user.updateDisabledNotificationCategories(disabledCategories == null || disabledCategories.isEmpty()
                ? null : String.join(",", disabledCategories));
        return UserDTO.from(userRepository.save(user));
    }

    private User findUser(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("We couldn't find that account."));
    }
}
