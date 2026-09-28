package za.co.unilinkhub.follow.domain;

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

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * A student following a business to keep an eye on new listings from them - one row per
 * (user, business) pair, the same shape as {@link za.co.unilinkhub.saved.domain.SavedListing}
 * but for providers rather than individual listings.
 */
@Entity
@Table(name = "followed_businesses", uniqueConstraints = {
        @UniqueConstraint(name = "uk_followed_businesses_user_business", columnNames = {"user_id", "business_id"})
})
@Getter
@NoArgsConstructor
public class FollowedBusiness {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "business_id", nullable = false)
    private UUID businessId;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private FollowedBusiness(UUID userId, UUID businessId) {
        this.userId = userId;
        this.businessId = businessId;
    }

    public static FollowedBusiness create(UUID userId, UUID businessId) {
        return new FollowedBusiness(userId, businessId);
    }
}
