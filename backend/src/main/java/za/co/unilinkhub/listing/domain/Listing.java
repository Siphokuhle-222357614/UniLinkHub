package za.co.unilinkhub.listing.domain;

import jakarta.persistence.Column;
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

    public void deactivate() {
        this.status = ListingStatus.INACTIVE;
    }

    public void reactivate() {
        this.status = ListingStatus.ACTIVE;
    }

    public void markSoldOut() {
        this.status = ListingStatus.SOLD_OUT;
    }

    public void recordView() {
        this.viewCount++;
    }
}
