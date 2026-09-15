package za.co.unilinkhub.savedsearch.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import za.co.unilinkhub.listing.domain.Listing;
import za.co.unilinkhub.listing.domain.ListingStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * A buyer's saved set of browse filters. When a matching listing is published, every saved
 * search with alerts enabled is checked against it (see SavedSearchService.notifyMatchingSearches)
 * so the buyer hears about it without having to keep re-running the same search by hand.
 */
@Entity
@Table(name = "saved_searches")
@Getter
@NoArgsConstructor
public class SavedSearch {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(nullable = false, length = 150)
    private String label;

    @Column(length = 150)
    private String keyword;

    @Column(length = 100)
    private String category;

    @Column(name = "max_price", precision = 10, scale = 2)
    private BigDecimal maxPrice;

    @Column(name = "listing_type", length = 20)
    private String listingType;

    @Column(name = "alerts_enabled", nullable = false)
    private boolean alertsEnabled = true;

    @Column(name = "last_viewed_at", nullable = false)
    private LocalDateTime lastViewedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private SavedSearch(UUID userId, String label, String keyword, String category, BigDecimal maxPrice, String listingType) {
        this.userId = userId;
        this.label = label;
        this.keyword = keyword;
        this.category = category;
        this.maxPrice = maxPrice;
        this.listingType = listingType;
        this.lastViewedAt = LocalDateTime.now();
    }

    public static SavedSearch create(UUID userId, String label, String keyword, String category,
                                      BigDecimal maxPrice, String listingType) {
        return new SavedSearch(userId, label, keyword, category, maxPrice, listingType);
    }

    public void setAlertsEnabled(boolean alertsEnabled) {
        this.alertsEnabled = alertsEnabled;
    }

    public void markViewed() {
        this.lastViewedAt = LocalDateTime.now();
    }

    public boolean matches(Listing listing) {
        if (listing.getStatus() != ListingStatus.ACTIVE) {
            return false;
        }
        if (category != null && !category.isBlank() && !category.equalsIgnoreCase(listing.getCategory())) {
            return false;
        }
        if (keyword != null && !keyword.isBlank()) {
            String needle = keyword.toLowerCase();
            if (!listing.getName().toLowerCase().contains(needle) && !listing.getDescription().toLowerCase().contains(needle)) {
                return false;
            }
        }
        if (maxPrice != null && listing.getPrice().compareTo(maxPrice) > 0) {
            return false;
        }
        if (listingType != null && !listingType.isBlank() && !listing.getClass().getSimpleName().equalsIgnoreCase(listingType)) {
            return false;
        }
        return true;
    }
}
