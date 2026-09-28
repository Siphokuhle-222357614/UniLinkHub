package za.co.unilinkhub.review.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;
import za.co.unilinkhub.review.domain.Review;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface JpaReviewRepository extends JpaRepository<Review, UUID>, za.co.unilinkhub.review.repository.ReviewRepository {
    Optional<Review> findByBusinessIdAndReviewerId(UUID businessId, UUID reviewerId);
    List<Review> findByBusinessId(UUID businessId);
    List<Review> findByFlaggedTrue();
}
