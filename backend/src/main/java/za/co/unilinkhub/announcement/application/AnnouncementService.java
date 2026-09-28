package za.co.unilinkhub.announcement.application;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import za.co.unilinkhub.announcement.domain.Announcement;
import za.co.unilinkhub.announcement.repository.AnnouncementRepository;
import za.co.unilinkhub.audit.application.AuditLogService;
import za.co.unilinkhub.common.exception.ResourceNotFoundException;
import za.co.unilinkhub.user.application.UserService;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AnnouncementService {

    private final AnnouncementRepository announcementRepository;
    private final AuditLogService auditLogService;
    private final UserService userService;

    public AnnouncementDTO publish(UUID adminId, String message, boolean active) {
        if (active) {
            for (Announcement existing : announcementRepository.findByActiveTrue()) {
                existing.deactivate();
                announcementRepository.save(existing);
            }
        }
        Announcement announcement = Announcement.publish(message, active);
        Announcement saved = announcementRepository.save(announcement);
        String adminName = userService.getById(adminId).firstName() + " " + userService.getById(adminId).lastName();
        auditLogService.record(adminName, "ANNOUNCEMENT", "Published announcement — \"" + truncate(message) + "\"");
        return AnnouncementDTO.from(saved);
    }

    public AnnouncementDTO deactivate(UUID adminId, UUID id) {
        Announcement announcement = announcementRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Announcement not found"));
        announcement.deactivate();
        Announcement saved = announcementRepository.save(announcement);
        String adminName = userService.getById(adminId).firstName() + " " + userService.getById(adminId).lastName();
        auditLogService.record(adminName, "ANNOUNCEMENT", "Deactivated announcement — \"" + truncate(announcement.getMessage()) + "\"");
        return AnnouncementDTO.from(saved);
    }

    public List<AnnouncementDTO> listAll() {
        return announcementRepository.findAll().stream()
                .sorted(Comparator.comparing(Announcement::getCreatedAt).reversed())
                .map(AnnouncementDTO::from)
                .toList();
    }

    public Optional<AnnouncementDTO> getActive() {
        return announcementRepository.findByActiveTrue().stream()
                .max(Comparator.comparing(Announcement::getCreatedAt))
                .map(AnnouncementDTO::from);
    }

    private String truncate(String message) {
        return message.length() > 60 ? message.substring(0, 60) + "..." : message;
    }
}
