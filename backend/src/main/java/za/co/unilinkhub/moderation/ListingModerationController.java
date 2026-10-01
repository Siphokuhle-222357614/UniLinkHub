package za.co.unilinkhub.moderation;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import za.co.unilinkhub.common.web.PageResponse;
import za.co.unilinkhub.listing.application.ListingDTO;
import za.co.unilinkhub.security.CurrentUser;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class ListingModerationController {

    private final ListingModerationService moderationService;

    public record TakeDownRequest(String reason) {
    }

    @GetMapping("/api/admin/listings")
    public PageResponse<ListingModerationService.AdminListingView> list(@RequestParam(required = false) String status,
                                                                        @RequestParam(required = false) String keyword,
                                                                        @RequestParam(required = false) UUID businessId,
                                                                        @RequestParam(defaultValue = "0") int page,
                                                                        @RequestParam(defaultValue = "30") int size) {
        return moderationService.list(status, keyword, businessId, page, size);
    }

    @PostMapping("/api/admin/listings/{id}/take-down")
    public ListingDTO takeDown(@CurrentUser UUID adminId, @PathVariable UUID id, @RequestBody(required = false) TakeDownRequest request) {
        return moderationService.takeDown(id, adminId, request == null ? null : request.reason());
    }

    @PostMapping("/api/admin/listings/{id}/restore")
    public ListingDTO restore(@CurrentUser UUID adminId, @PathVariable UUID id) {
        return moderationService.restore(id, adminId);
    }
}
