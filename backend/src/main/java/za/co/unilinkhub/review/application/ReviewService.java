package za.co.unilinkhub.review.application;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import za.co.unilinkhub.audit.application.AuditLogService;
import za.co.unilinkhub.business.domain.Business;
import za.co.unilinkhub.business.repository.BusinessRepository;
import za.co.unilinkhub.common.exception.BadRequestException;
import za.co.unilinkhub.common.exception.ResourceNotFoundException;
import za.co.unilinkhub.notification.application.NotificationService;
import za.co.unilinkhub.review.domain.Review;
import za.co.unilinkhub.review.repository.ReviewRepository;
import za.co.unilinkhub.user.domain.User;
import za.co.unilinkhub.user.repository.UserRepository;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final BusinessRepository businessRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final AuditLogService auditLogService;

    public ReviewView upsert(UUID reviewerId, UUID businessId, int rating, String comment) {
        if (rating < 1 || rating > 5) {
            throw new BadRequestException("Rating must be between 1 and 5");
        }
        Business business = businessRepository.findById(businessId)
                .orElseThrow(() -> new ResourceNotFoundException("Business not found"));

        Review review = reviewRepository.findByBusinessIdAndReviewerId(businessId, reviewerId).orElse(null);
        boolean isNew = review == null;
        if (isNew) {
            review = Review.create(businessId, reviewerId, rating, comment);
        } else {
            review.update(rating, comment);
        }
        Review saved = reviewRepository.save(review);

        if (isNew) {
            User reviewer = userRepository.findById(reviewerId).orElse(null);
            String reviewerName = reviewer == null ? "A student" : reviewer.getFullName();
            notificationService.notify(business.getOwnerId(), "REVIEW",
                    reviewerName + " left a " + rating + "-star review on \"" + business.getBusinessName() + "\"");
        }

        return toView(saved);
    }

    public BusinessReviewsDTO listForBusiness(UUID businessId) {
        List<Review> reviews = reviewRepository.findByBusinessId(businessId).stream()
                .sorted(Comparator.comparing(Review::getCreatedAt).reversed())
                .toList();

        double average = reviews.isEmpty() ? 0 : reviews.stream().mapToInt(Review::getRating).average().orElse(0);
        Map<Integer, Long> distribution = reviews.stream()
                .collect(Collectors.groupingBy(Review::getRating, Collectors.counting()));

        List<ReviewView> views = reviews.stream().map(this::toView).toList();
        return new BusinessReviewsDTO(Math.round(average * 10) / 10.0, reviews.size(), distribution, views);
    }

    public void flag(UUID reviewId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found"));
        review.flag();
        reviewRepository.save(review);
    }

    public List<ReviewView> adminList(boolean flaggedOnly) {
        List<Review> reviews = flaggedOnly ? reviewRepository.findByFlaggedTrue() : reviewRepository.findAll();
        return reviews.stream()
                .sorted(Comparator.comparing(Review::getCreatedAt).reversed())
                .map(this::toView)
                .toList();
    }

    public void adminRemove(UUID reviewId, UUID adminId) {
        Review review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found"));
        String businessName = businessRepository.findById(review.getBusinessId())
                .map(Business::getBusinessName).orElse("Unknown business");
        reviewRepository.deleteById(reviewId);
        String adminName = userRepository.findById(adminId).map(User::getFullName).orElse("Unknown admin");
        auditLogService.record(adminName, "REVIEW", "Removed a review on \"" + businessName + "\"");
    }

    public ReviewStatsDTO adminStats() {
        List<Review> all = reviewRepository.findAll();
        double platformAverage = all.isEmpty() ? 0 : all.stream().mapToInt(Review::getRating).average().orElse(0);
        long flaggedCount = all.stream().filter(Review::isFlagged).count();

        Map<UUID, List<Review>> byBusiness = all.stream().collect(Collectors.groupingBy(Review::getBusinessId));

        List<ReviewStatsDTO.BusinessRating> ratings = byBusiness.entrySet().stream()
                .map(e -> {
                    String name = businessRepository.findById(e.getKey()).map(Business::getBusinessName).orElse("Unknown business");
                    double avg = e.getValue().stream().mapToInt(Review::getRating).average().orElse(0);
                    return new ReviewStatsDTO.BusinessRating(name, Math.round(avg * 10) / 10.0, e.getValue().size());
                })
                .toList();

        List<ReviewStatsDTO.BusinessRating> topRated = ratings.stream()
                .sorted(Comparator.comparingDouble(ReviewStatsDTO.BusinessRating::average).reversed())
                .limit(5)
                .toList();
        List<ReviewStatsDTO.BusinessRating> lowestRated = ratings.stream()
                .sorted(Comparator.comparingDouble(ReviewStatsDTO.BusinessRating::average))
                .limit(5)
                .toList();

        return new ReviewStatsDTO(Math.round(platformAverage * 10) / 10.0, all.size(), flaggedCount,
                byBusiness.size(), topRated, lowestRated);
    }

    private ReviewView toView(Review review) {
        String businessName = businessRepository.findById(review.getBusinessId())
                .map(Business::getBusinessName).orElse("Unknown business");
        String reviewerName = userRepository.findById(review.getReviewerId())
                .map(User::getFullName).orElse("Deleted account");
        return ReviewView.of(review, businessName, reviewerName);
    }
}
