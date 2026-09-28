package za.co.unilinkhub.notification.application;

import za.co.unilinkhub.notification.domain.Notification;

import java.time.LocalDateTime;
import java.util.UUID;

public record NotificationDTO(
        UUID id,
        String category,
        String message,
        boolean read,
        LocalDateTime createdAt
) {
    public static NotificationDTO from(Notification notification) {
        return new NotificationDTO(notification.getId(), notification.getCategory(), notification.getMessage(),
                notification.isRead(), notification.getCreatedAt());
    }
}
