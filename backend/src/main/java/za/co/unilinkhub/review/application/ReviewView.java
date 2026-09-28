package za.co.unilinkhub.review.application;

import za.co.unilinkhub.review.domain.Review;

import java.time.LocalDateTime;
import java.util.UUID;

public record ReviewView(
        UUID id,
        UUID businessId,
        String businessName,
        UUID reviewerId,
        String reviewerName,
        int rating,
        String comment,
        boolean flagged,
        int flagCount,
        LocalDateTime createdAt
) {
    public static ReviewView of(Review review, String businessName, String reviewerName) {
        return new ReviewView(review.getId(), review.getBusinessId(), businessName, review.getReviewerId(),
                reviewerName, review.getRating(), review.getComment(), review.isFlagged(), review.getFlagCount(),
                review.getCreatedAt());
    }
}
