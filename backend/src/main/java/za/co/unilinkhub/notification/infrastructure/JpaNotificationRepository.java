package za.co.unilinkhub.notification.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;
import za.co.unilinkhub.notification.domain.Notification;

import java.util.List;
import java.util.UUID;

public interface JpaNotificationRepository extends JpaRepository<Notification, UUID>, za.co.unilinkhub.notification.repository.NotificationRepository {
    List<Notification> findByUserId(UUID userId);
    long countByUserIdAndReadFalse(UUID userId);
}
