package za.co.unilinkhub.user.web;

import za.co.unilinkhub.user.application.UserDTO;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record UserResponse(
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
    public static UserResponse from(UserDTO dto) {
        return new UserResponse(
                dto.id(), dto.studentNumber(), dto.firstName(), dto.lastName(),
                dto.email(), dto.pendingEmail(), dto.phoneNumber(), dto.role(), dto.accountStatus(),
                dto.seller(), dto.suspensionReason(), dto.disabledNotificationCategories(), dto.createdAt()
        );
    }
}
