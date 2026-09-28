package za.co.unilinkhub.notification.web;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import za.co.unilinkhub.notification.application.NotificationDTO;
import za.co.unilinkhub.notification.application.NotificationService;
import za.co.unilinkhub.security.CurrentUser;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    public record BroadcastRequest(@NotBlank String audience, @NotBlank String message) {
    }

    @GetMapping("/api/notifications")
    public List<NotificationDTO> mine(@CurrentUser UUID userId) {
        return notificationService.listMine(userId);
    }

    @GetMapping("/api/notifications/unread-count")
    public Map<String, Long> unreadCount(@CurrentUser UUID userId) {
        return Map.of("count", notificationService.unreadCount(userId));
    }

    @PostMapping("/api/notifications/mark-all-read")
    public void markAllRead(@CurrentUser UUID userId) {
        notificationService.markAllRead(userId);
    }

    @PostMapping("/api/admin/notifications/broadcast")
    @PreAuthorize("hasRole('ADMIN')")
    public Map<String, Integer> broadcast(@CurrentUser UUID adminId, @Valid @RequestBody BroadcastRequest request) {
        return Map.of("recipientCount", notificationService.broadcast(adminId, request.audience(), request.message()));
    }
}
