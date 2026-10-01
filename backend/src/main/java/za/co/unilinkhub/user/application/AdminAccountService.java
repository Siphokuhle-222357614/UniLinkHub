package za.co.unilinkhub.user.application;

import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import za.co.unilinkhub.audit.application.AuditLogService;
import za.co.unilinkhub.common.exception.BadRequestException;
import za.co.unilinkhub.common.exception.ConflictException;
import za.co.unilinkhub.common.exception.ResourceNotFoundException;
import za.co.unilinkhub.mail.AccountEmails;
import za.co.unilinkhub.user.domain.User;
import za.co.unilinkhub.user.domain.UserRole;
import za.co.unilinkhub.user.repository.UserRepository;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * Admin accounts are separate from student accounts: a student account can never be turned into
 * an admin. New admins are invited by an existing admin - the invite email carries a one-time link
 * to set a password - and the very first admin is created from configuration at startup
 * (AdminBootstrap). This keeps admins neutral: they can't own businesses, buy, sell or review.
 */
@Service
@RequiredArgsConstructor
public class AdminAccountService {

    private static final Duration INVITE_LINK_LIFETIME = Duration.ofHours(72);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AccountEmails accountEmails;
    private final AuditLogService auditLogService;

    public List<UserDTO> listAdmins() {
        return userRepository.findByRole(UserRole.ADMIN).stream()
                .sorted(Comparator.comparing(User::getCreatedAt))
                .map(UserDTO::from)
                .toList();
    }

    public UserDTO invite(UUID invitedById, String firstName, String lastName, String email) {
        String normalized = StudentEmailPolicy.normalize(email);
        if (normalized == null || !normalized.contains("@")) {
            throw new BadRequestException("Please enter a real email address for the new admin.");
        }
        if (userRepository.existsByEmail(normalized)) {
            throw new ConflictException("There's already an account using " + normalized + ". Admin accounts must use an email "
                    + "address that isn't already a student account - for example a staff address.");
        }
        User inviter = userRepository.findById(invitedById)
                .orElseThrow(() -> new ResourceNotFoundException("We couldn't find your admin account."));

        // Nobody knows this password; the invitee sets their own through the emailed link.
        User admin = User.createAdmin(firstName.trim(), lastName.trim(), normalized,
                passwordEncoder.encode(UUID.randomUUID().toString()));
        String token = UUID.randomUUID().toString();
        admin.requestPasswordReset(token, LocalDateTime.now().plus(INVITE_LINK_LIFETIME));
        User saved = userRepository.save(admin);

        accountEmails.sendAdminInvite(saved, inviter.getFullName(), token);
        auditLogService.record(inviter.getFullName(), "ACCOUNT", "Invited \"" + saved.getFullName() + "\" (" + normalized + ") as an admin");
        return UserDTO.from(saved);
    }

    /** Startup bootstrap: create the first admin if there isn't one yet. */
    public boolean createInitialAdminIfMissing(String email, String rawPassword, String firstName, String lastName) {
        if (!userRepository.findByRole(UserRole.ADMIN).isEmpty()) {
            return false;
        }
        String normalized = StudentEmailPolicy.normalize(email);
        if (userRepository.existsByEmail(normalized)) {
            throw new IllegalStateException("Can't create the first admin: " + normalized + " is already used by a student account. "
                    + "Set ADMIN_EMAIL to an address that isn't registered yet.");
        }
        userRepository.save(User.createAdmin(firstName, lastName, normalized, passwordEncoder.encode(rawPassword)));
        return true;
    }
}
