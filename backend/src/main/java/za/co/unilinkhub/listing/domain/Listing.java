package za.co.unilinkhub.listing.domain;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import org.hibernate.annotations.BatchSize;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.DiscriminatorType;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Base type for anything a business offers. Concrete listings are {@link Product} or
 * {@link Service} (see the class diagram) - Listing itself is never instantiated directly.
 * A `category` field was added beyond the original diagram because browse/search by category
 * is an explicit MVP requirement (Section 11.1 of the project docs).
 */
@Entity
@Table(name = "listings")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "listing_type", discriminatorType = DiscriminatorType.STRING)
@Getter
@NoArgsConstructor
public abstract class Listing {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "business_id", nullable = false)
    private UUID businessId;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(nullable = false, length = 2000)
    private String description;

    @Column(nullable = false, length = 100)
    private String category;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ListingStatus status = ListingStatus.ACTIVE;

    @Column(name = "view_count", nullable = false)
    private long viewCount = 0;

    /** The cover photo - always the first of {@link #photos}. Kept as its own column so older code and data still work. */
    @Column(name = "image_url")
    private String imageUrl;

    /** Up to {@value #MAX_PHOTOS} photos in display order. Empty for listings from before galleries existed. */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "listing_photos", joinColumns = @JoinColumn(name = "listing_id"))
    @OrderColumn(name = "position")
    @Column(name = "url", nullable = false)
    @BatchSize(size = 50)
    private List<String> photos = new ArrayList<>();

    /**
     * Set when an admin removes this listing for breaking the marketplace rules. While set, the
     * listing stays hidden from everyone but its owner and admins - the seller can't switch it
     * back on, and restocking it doesn't make it visible again. Only an admin can restore it.
     */
    @Column(name = "takedown_reason", length = 1000)
    private String takedownReason;

    @Column(name = "taken_down_at")
    private LocalDateTime takenDownAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    protected Listing(UUID businessId, String name, String description, String category, BigDecimal price) {
        this.businessId = businessId;
        this.name = name;
        this.description = description;
        this.category = category;
        this.price = price;
    }

    public void updateBasicDetails(String name, String description, String category, BigDecimal price) {
        if (name != null && !name.isBlank()) {
            this.name = name;
        }
        if (description != null && !description.isBlank()) {
            this.description = description;
        }
        if (category != null && !category.isBlank()) {
            this.category = category;
        }
        if (price != null) {
            this.price = price;
        }
    }

    public static final int MAX_PHOTOS = 6;

    /** A single photo (the older one-image API): becomes the whole gallery. */
    public void updateImageUrl(String imageUrl) {
        updatePhotos(imageUrl == null ? List.of() : List.of(imageUrl));
    }

    public void updatePhotos(List<String> urls) {
        this.photos.clear();
        this.photos.addAll(urls);
        this.imageUrl = urls.isEmpty() ? null : urls.get(0);
    }

    /** The gallery to show: the photos, or just the cover for listings from before galleries existed. */
    public List<String> getGallery() {
        if (!photos.isEmpty()) {
            return List.copyOf(photos);
        }
        return imageUrl == null ? List.of() : List.of(imageUrl);
    }

    public void deactivate() {
        this.status = ListingStatus.INACTIVE;
    }

    public void reactivate() {
        if (!isTakenDown()) {
            this.status = ListingStatus.ACTIVE;
        }
    }

    public void markSoldOut() {
        if (!isTakenDown()) {
            this.status = ListingStatus.SOLD_OUT;
        }
    }

    public boolean isTakenDown() {
        return takenDownAt != null;
    }

    public void takeDown(String reason, LocalDateTime when) {
        this.takedownReason = reason;
        this.takenDownAt = when;
        this.status = ListingStatus.INACTIVE;
    }

    /** Admin undoes a take-down. The listing comes back hidden; the seller decides when to show it again. */
    public void restoreAfterTakedown() {
        this.takedownReason = null;
        this.takenDownAt = null;
    }

    public void recordView() {
        this.viewCount++;
    }
}
