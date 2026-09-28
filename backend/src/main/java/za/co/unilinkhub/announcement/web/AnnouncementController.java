package za.co.unilinkhub.announcement.web;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import za.co.unilinkhub.announcement.application.AnnouncementDTO;
import za.co.unilinkhub.announcement.application.AnnouncementService;
import za.co.unilinkhub.security.CurrentUser;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class AnnouncementController {

    private final AnnouncementService announcementService;

    public record PublishRequest(@NotBlank String message, boolean active) {
    }

    @PostMapping("/api/admin/announcements")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public AnnouncementDTO publish(@CurrentUser UUID adminId, @Valid @RequestBody PublishRequest request) {
        return announcementService.publish(adminId, request.message(), request.active());
    }

    @GetMapping("/api/admin/announcements")
    @PreAuthorize("hasRole('ADMIN')")
    public List<AnnouncementDTO> listAll() {
        return announcementService.listAll();
    }

    @PostMapping("/api/admin/announcements/{id}/deactivate")
    @PreAuthorize("hasRole('ADMIN')")
    public AnnouncementDTO deactivate(@CurrentUser UUID adminId, @PathVariable UUID id) {
        return announcementService.deactivate(adminId, id);
    }

    @GetMapping("/api/announcements/active")
    public AnnouncementDTO active() {
        return announcementService.getActive().orElse(null);
    }
}
