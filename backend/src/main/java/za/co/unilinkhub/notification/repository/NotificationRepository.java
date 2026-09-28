package za.co.unilinkhub.notification.repository;

import za.co.unilinkhub.notification.domain.Notification;

import java.util.List;
import java.util.UUID;

public interface NotificationRepository {
    Notification save(Notification notification);
    List<Notification> findByUserId(UUID userId);
    long countByUserIdAndReadFalse(UUID userId);
}
