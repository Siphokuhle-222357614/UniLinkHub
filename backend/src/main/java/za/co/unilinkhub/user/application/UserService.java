package za.co.unilinkhub.user.application;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import za.co.unilinkhub.audit.application.AuditLogService;
import za.co.unilinkhub.common.exception.BadRequestException;
import za.co.unilinkhub.common.exception.ResourceNotFoundException;
import za.co.unilinkhub.user.domain.AccountStatus;
import za.co.unilinkhub.user.domain.User;
import za.co.unilinkhub.user.repository.UserRepository;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final AuditLogService auditLogService;

    public UserDTO getById(UUID id) {
        return UserDTO.from(findUser(id));
    }

    public UserDTO verifyEmail(String token) {
        User user = userRepository.findByVerificationToken(token)
                .orElseThrow(() -> new BadRequestException("Invalid or expired verification token"));
        user.verifyEmail(token);
        return UserDTO.from(userRepository.save(user));
    }

    public UserDTO becomeSeller(UUID userId) {
        User user = findUser(userId);
        user.becomeSeller();
        return UserDTO.from(userRepository.save(user));
    }

    public List<UserDTO> listPendingAccounts() {
        return userRepository.findByAccountStatus(AccountStatus.PENDING_VERIFICATION).stream()
                .map(UserDTO::from)
                .toList();
    }

    public UserDTO approveAccount(UUID userId) {
        User user = findUser(userId);
        user.approve();
        return UserDTO.from(userRepository.save(user));
    }

    public List<UserDTO> listAccounts(String status, String keyword) {
        List<User> users = userRepository.findAll();

        if (status != null && !status.isBlank() && !"ALL".equalsIgnoreCase(status)) {
            AccountStatus filter = AccountStatus.valueOf(status.toUpperCase());
            users = users.stream().filter(u -> u.getAccountStatus() == filter).toList();
        }

        if (keyword != null && !keyword.isBlank()) {
            String needle = keyword.toLowerCase();
            users = users.stream()
                    .filter(u -> u.getFullName().toLowerCase().contains(needle)
                            || u.getEmail().toLowerCase().contains(needle)
                            || u.getStudentNumber().toLowerCase().contains(needle))
                    .toList();
        }

        return users.stream()
                .sorted(Comparator.comparing(User::getCreatedAt).reversed())
                .map(UserDTO::from)
                .toList();
    }

    public UserDTO suspendAccount(UUID userId, UUID adminId, String reason) {
        User user = findUser(userId);
        user.suspend(reason);
        UserDTO dto = UserDTO.from(userRepository.save(user));
        auditLogService.record(findUser(adminId).getFullName(), "ACCOUNT", "Suspended account \"" + user.getFullName() + "\""
                + (reason != null && !reason.isBlank() ? " — reason: " + reason : ""));
        return dto;
    }

    public UserDTO reactivateAccount(UUID userId, UUID adminId) {
        User user = findUser(userId);
        user.reactivate();
        UserDTO dto = UserDTO.from(userRepository.save(user));
        auditLogService.record(findUser(adminId).getFullName(), "ACCOUNT", "Reactivated account \"" + user.getFullName() + "\"");
        return dto;
    }

    public UserDTO promoteToAdmin(UUID userId, UUID adminId) {
        User user = findUser(userId);
        user.promoteToAdmin();
        UserDTO dto = UserDTO.from(userRepository.save(user));
        auditLogService.record(findUser(adminId).getFullName(), "ACCOUNT", "Promoted \"" + user.getFullName() + "\" to Admin");
        return dto;
    }

    public UserDTO updateNotificationPreferences(UUID userId, java.util.List<String> disabledCategories) {
        User user = findUser(userId);
        user.updateDisabledNotificationCategories(disabledCategories == null || disabledCategories.isEmpty()
                ? null : String.join(",", disabledCategories));
        return UserDTO.from(userRepository.save(user));
    }

    private User findUser(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }
}
