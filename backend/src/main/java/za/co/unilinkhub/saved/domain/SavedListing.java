package za.co.unilinkhub.saved.domain;

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
 * A student bookmarking a listing for later - the Buyer Dashboard's "saved/favourited
 * listings" from Section 11.1 of the project docs. One row per (user, listing) pair.
 */
@Entity
@Table(name = "saved_listings", uniqueConstraints = {
        @UniqueConstraint(name = "uk_saved_listings_user_listing", columnNames = {"user_id", "listing_id"})
})
@Getter
@NoArgsConstructor
public class SavedListing {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "listing_id", nullable = false)
    private UUID listingId;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private SavedListing(UUID userId, UUID listingId) {
        this.userId = userId;
        this.listingId = listingId;
    }

    public static SavedListing create(UUID userId, UUID listingId) {
        return new SavedListing(userId, listingId);
    }
}
