package za.co.unilinkhub.review.web;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import za.co.unilinkhub.review.application.BusinessReviewsDTO;
import za.co.unilinkhub.review.application.ReviewService;
import za.co.unilinkhub.review.application.ReviewStatsDTO;
import za.co.unilinkhub.review.application.ReviewView;
import za.co.unilinkhub.security.CurrentUser;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class ReviewController {

    private final ReviewService reviewService;

    public record ReviewRequest(@Min(1) @Max(5) int rating, String comment) {
    }

    @PostMapping("/api/businesses/{id}/reviews")
    public ReviewView upsert(@CurrentUser UUID reviewerId, @PathVariable UUID id, @Valid @RequestBody ReviewRequest request) {
        return reviewService.upsert(reviewerId, id, request.rating(), request.comment());
    }

    @GetMapping("/api/businesses/{id}/reviews")
    public BusinessReviewsDTO listForBusiness(@PathVariable UUID id) {
        return reviewService.listForBusiness(id);
    }

    @PostMapping("/api/reviews/{id}/flag")
    public void flag(@PathVariable UUID id) {
        reviewService.flag(id);
    }

    @GetMapping("/api/admin/reviews")
    @PreAuthorize("hasRole('ADMIN')")
    public List<ReviewView> adminList(@RequestParam(defaultValue = "false") boolean flaggedOnly) {
        return reviewService.adminList(flaggedOnly);
    }

    @DeleteMapping("/api/admin/reviews/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public void adminRemove(@CurrentUser UUID adminId, @PathVariable UUID id) {
        reviewService.adminRemove(id, adminId);
    }

    @GetMapping("/api/admin/reviews/stats")
    @PreAuthorize("hasRole('ADMIN')")
    public ReviewStatsDTO adminStats() {
        return reviewService.adminStats();
    }
}
