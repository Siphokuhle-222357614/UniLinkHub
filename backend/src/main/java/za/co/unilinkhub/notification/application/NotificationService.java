package za.co.unilinkhub.notification.application;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import za.co.unilinkhub.audit.application.AuditLogService;
import za.co.unilinkhub.business.domain.Business;
import za.co.unilinkhub.business.domain.VerificationStatus;
import za.co.unilinkhub.business.repository.BusinessRepository;
import za.co.unilinkhub.notification.domain.Notification;
import za.co.unilinkhub.notification.repository.NotificationRepository;
import za.co.unilinkhub.user.domain.User;
import za.co.unilinkhub.user.repository.UserRepository;

import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final BusinessRepository businessRepository;
    private final AuditLogService auditLogService;

    public void notify(UUID userId, String category, String message) {
        boolean enabled = userRepository.findById(userId)
                .map(u -> u.isNotificationCategoryEnabled(category))
                .orElse(true);
        if (!enabled) {
            return;
        }
        notificationRepository.save(Notification.create(userId, category, message));
    }

    public int broadcast(UUID adminId, String audience, String message) {
        List<User> recipients = switch (audience == null ? "" : audience.toUpperCase()) {
            case "ALL_SELLERS" -> userRepository.findAll().stream().filter(User::isSeller).toList();
            case "PENDING_BUSINESS_OWNERS" -> {
                Set<UUID> ownerIds = businessRepository.findByVerificationStatus(VerificationStatus.PENDING).stream()
                        .map(Business::getOwnerId)
                        .collect(Collectors.toSet());
                yield userRepository.findAll().stream().filter(u -> ownerIds.contains(u.getId())).toList();
            }
            default -> userRepository.findAll();
        };

        for (User recipient : recipients) {
            notify(recipient.getId(), "ANNOUNCEMENT", message);
        }

        String adminName = userRepository.findById(adminId).map(User::getFullName).orElse("Unknown admin");
        auditLogService.record(adminName, "ANNOUNCEMENT",
                "Sent a notification to " + recipients.size() + " user(s) (" + audience + "): \""
                        + (message.length() > 60 ? message.substring(0, 60) + "..." : message) + "\"");

        return recipients.size();
    }

    public List<NotificationDTO> listMine(UUID userId) {
        return notificationRepository.findByUserId(userId).stream()
                .sorted(Comparator.comparing(Notification::getCreatedAt).reversed())
                .map(NotificationDTO::from)
                .toList();
    }

    public long unreadCount(UUID userId) {
        return notificationRepository.countByUserIdAndReadFalse(userId);
    }

    public void markAllRead(UUID userId) {
        for (Notification n : notificationRepository.findByUserId(userId)) {
            if (!n.isRead()) {
                n.markRead();
                notificationRepository.save(n);
            }
        }
    }
}
