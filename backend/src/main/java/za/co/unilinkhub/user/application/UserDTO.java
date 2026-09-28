package za.co.unilinkhub.user.application;

import za.co.unilinkhub.user.domain.User;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public record UserDTO(
        UUID id,
        String studentNumber,
        String firstName,
        String lastName,
        String email,
        String pendingEmail,
        String phoneNumber,
        String role,
        String accountStatus,
        boolean seller,
        String suspensionReason,
        List<String> disabledNotificationCategories,
        LocalDateTime createdAt
) {
    public static UserDTO from(User user) {
        String disabled = user.getDisabledNotificationCategories();
        List<String> disabledList = disabled == null || disabled.isBlank()
                ? List.of()
                : Arrays.stream(disabled.split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList();

        return new UserDTO(
                user.getId(),
                user.getStudentNumber(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                user.getPendingEmail(),
                user.getPhoneNumber(),
                user.getRole().name(),
                user.getAccountStatus().name(),
                user.isSeller(),
                user.getSuspensionReason(),
                disabledList,
                user.getCreatedAt()
        );
    }
}
