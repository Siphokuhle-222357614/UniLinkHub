package za.co.unilinkhub.savedsearch.web;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import za.co.unilinkhub.savedsearch.application.SavedSearchDTO;
import za.co.unilinkhub.savedsearch.application.SavedSearchService;
import za.co.unilinkhub.security.CurrentUser;
import za.co.unilinkhub.security.StudentOnly;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/saved-searches")
@RequiredArgsConstructor
public class SavedSearchController {

    private final SavedSearchService savedSearchService;

    public record SaveRequest(@NotBlank String label, String keyword, String category, BigDecimal maxPrice, String listingType) {
    }

    public record AlertsRequest(boolean alertsEnabled) {
    }

    @StudentOnly("save searches")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SavedSearchDTO save(@CurrentUser UUID userId, @Valid @RequestBody SaveRequest request) {
        return savedSearchService.save(userId, request.label(), request.keyword(), request.category(), request.maxPrice(), request.listingType());
    }

    @GetMapping
    public List<SavedSearchDTO> mine(@CurrentUser UUID userId) {
        return savedSearchService.listMine(userId);
    }

    @PatchMapping("/{id}")
    public SavedSearchDTO setAlerts(@CurrentUser UUID userId, @PathVariable UUID id, @RequestBody AlertsRequest request) {
        return savedSearchService.setAlertsEnabled(userId, id, request.alertsEnabled());
    }

    @PostMapping("/{id}/view")
    public SavedSearchDTO markViewed(@CurrentUser UUID userId, @PathVariable UUID id) {
        return savedSearchService.markViewed(userId, id);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@CurrentUser UUID userId, @PathVariable UUID id) {
        savedSearchService.delete(userId, id);
    }
}
