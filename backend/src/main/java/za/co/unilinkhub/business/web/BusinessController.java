package za.co.unilinkhub.business.web;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import za.co.unilinkhub.business.application.AdminBusinessView;
import za.co.unilinkhub.business.application.BusinessContactDTO;
import za.co.unilinkhub.business.application.BusinessDTO;
import za.co.unilinkhub.business.application.BusinessService;
import za.co.unilinkhub.business.application.BusinessStatsDTO;
import za.co.unilinkhub.business.application.ProviderProfileDTO;
import za.co.unilinkhub.follow.application.FollowService;
import za.co.unilinkhub.security.CurrentUser;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class BusinessController {

    private final BusinessService businessService;
    private final FollowService followService;

    public record CreateBusinessRequest(
            @NotBlank String businessName,
            @NotBlank String description,
            @NotBlank String category
    ) {
    }

    public record UpdateBusinessRequest(String businessName, String description, String category, String imageUrl) {
    }

    public record RejectRequest(String reason) {
    }

    @PostMapping("/api/businesses")
    @ResponseStatus(HttpStatus.CREATED)
    public BusinessDTO create(@CurrentUser UUID userId, @Valid @RequestBody CreateBusinessRequest request) {
        return businessService.create(userId, request.businessName(), request.description(), request.category());
    }

    @PatchMapping("/api/businesses/{id}")
    public BusinessDTO update(@CurrentUser UUID userId, @PathVariable UUID id, @RequestBody UpdateBusinessRequest request) {
        return businessService.update(id, userId, request.businessName(), request.description(),
                request.category(), request.imageUrl());
    }

    @PostMapping("/api/businesses/{id}/request-verification")
    public BusinessDTO requestVerification(@CurrentUser UUID userId, @PathVariable UUID id) {
        return businessService.requestVerification(id, userId);
    }

    @GetMapping("/api/businesses/{id}")
    public BusinessDTO getById(@PathVariable UUID id) {
        return businessService.getById(id);
    }

    @GetMapping("/api/businesses")
    public List<ProviderProfileDTO> directory(@RequestParam(required = false) String keyword,
                                               @RequestParam(required = false) String category,
                                               @RequestParam(defaultValue = "false") boolean verifiedOnly) {
        return businessService.listPublic(keyword, category, verifiedOnly);
    }

    @GetMapping("/api/businesses/{id}/profile")
    public ProviderProfileDTO profile(@PathVariable UUID id) {
        return businessService.getProviderProfile(id);
    }

    @GetMapping("/api/businesses/mine")
    public List<BusinessDTO> mine(@CurrentUser UUID userId) {
        return businessService.getByOwner(userId);
    }

    @GetMapping("/api/businesses/{id}/stats")
    public BusinessStatsDTO stats(@CurrentUser UUID userId, @PathVariable UUID id) {
        return businessService.getStats(id, userId);
    }

    @GetMapping("/api/businesses/{id}/similar")
    public List<ProviderProfileDTO> similar(@PathVariable UUID id) {
        return businessService.listSimilar(id);
    }

    @GetMapping("/api/businesses/{id}/contact")
    public BusinessContactDTO contact(@CurrentUser UUID userId, @PathVariable UUID id) {
        return businessService.getContact(id);
    }

    @PostMapping("/api/businesses/{id}/follow")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void follow(@CurrentUser UUID userId, @PathVariable UUID id) {
        followService.follow(userId, id);
    }

    @DeleteMapping("/api/businesses/{id}/follow")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unfollow(@CurrentUser UUID userId, @PathVariable UUID id) {
        followService.unfollow(userId, id);
    }

    @GetMapping("/api/businesses/followed/mine")
    public List<ProviderProfileDTO> followedMine(@CurrentUser UUID userId) {
        return followService.mine(userId);
    }

    @GetMapping("/api/admin/businesses")
    @PreAuthorize("hasRole('ADMIN')")
    public List<AdminBusinessView> adminBusinesses(@RequestParam(defaultValue = "PENDING") String status,
                                                     @RequestParam(required = false) String keyword) {
        return businessService.listForAdmin(status, keyword);
    }

    @GetMapping("/api/admin/businesses/recently-decided")
    @PreAuthorize("hasRole('ADMIN')")
    public List<AdminBusinessView> recentlyDecided(@RequestParam(defaultValue = "5") int limit) {
        return businessService.recentlyDecided(limit);
    }

    @PostMapping("/api/admin/businesses/{id}/verify")
    @PreAuthorize("hasRole('ADMIN')")
    public BusinessDTO verify(@CurrentUser UUID adminId, @PathVariable UUID id) {
        return businessService.verify(id, adminId);
    }

    @PostMapping("/api/admin/businesses/{id}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    public BusinessDTO reject(@CurrentUser UUID adminId, @PathVariable UUID id, @RequestBody(required = false) RejectRequest request) {
        return businessService.reject(id, request == null ? null : request.reason(), adminId);
    }
}
