package za.co.unilinkhub.review.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * A star rating + comment a student leaves on a business. One per (business, reviewer) - a
 * second submission edits the first rather than creating a duplicate, since there's no purchase/
 * order record to tie a review to a specific transaction.
 */
@Entity
@Table(name = "reviews", uniqueConstraints = {
        @UniqueConstraint(name = "uk_reviews_business_reviewer", columnNames = {"business_id", "reviewer_id"})
})
@Getter
@NoArgsConstructor
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "business_id", nullable = false)
    private UUID businessId;

    @Column(name = "reviewer_id", nullable = false)
    private UUID reviewerId;

    @Column(nullable = false)
    private int rating;

    @Column(length = 1000)
    private String comment;

    @Column(nullable = false)
    private boolean flagged = false;

    @Column(name = "flag_count", nullable = false)
    private int flagCount = 0;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    private Review(UUID businessId, UUID reviewerId, int rating, String comment) {
        this.businessId = businessId;
        this.reviewerId = reviewerId;
        this.rating = rating;
        this.comment = comment;
    }

    public static Review create(UUID businessId, UUID reviewerId, int rating, String comment) {
        return new Review(businessId, reviewerId, rating, comment);
    }

    public void update(int rating, String comment) {
        this.rating = rating;
        this.comment = comment;
    }

    public void flag() {
        this.flagCount++;
        this.flagged = true;
    }
}
