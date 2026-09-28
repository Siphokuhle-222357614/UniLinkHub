package za.co.unilinkhub.announcement.application;

import za.co.unilinkhub.announcement.domain.Announcement;

import java.time.LocalDateTime;
import java.util.UUID;

public record AnnouncementDTO(
        UUID id,
        String message,
        boolean active,
        LocalDateTime createdAt
) {
    public static AnnouncementDTO from(Announcement announcement) {
        return new AnnouncementDTO(announcement.getId(), announcement.getMessage(), announcement.isActive(),
                announcement.getCreatedAt());
    }
}
