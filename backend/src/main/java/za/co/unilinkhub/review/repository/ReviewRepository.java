package za.co.unilinkhub.review.repository;

import za.co.unilinkhub.review.domain.Review;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ReviewRepository {
    Review save(Review review);
    Optional<Review> findById(UUID id);
    Optional<Review> findByBusinessIdAndReviewerId(UUID businessId, UUID reviewerId);
    List<Review> findByBusinessId(UUID businessId);
    List<Review> findByFlaggedTrue();
    List<Review> findAll();
    void deleteById(UUID id);
}
