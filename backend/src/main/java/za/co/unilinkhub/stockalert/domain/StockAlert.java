package za.co.unilinkhub.stockalert.domain;

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
 * A "notify me when back in stock" subscription a buyer leaves on a sold-out product. Cleared
 * (all rows for the listing deleted) the moment the listing restocks, after notifying everyone
 * subscribed - this is a one-shot alert, not an ongoing watch.
 */
@Entity
@Table(name = "stock_alerts", uniqueConstraints = {
        @UniqueConstraint(name = "uk_stock_alerts_listing_user", columnNames = {"listing_id", "user_id"})
})
@Getter
@NoArgsConstructor
public class StockAlert {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "listing_id", nullable = false)
    private UUID listingId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private StockAlert(UUID listingId, UUID userId) {
        this.listingId = listingId;
        this.userId = userId;
    }

    public static StockAlert subscribe(UUID listingId, UUID userId) {
        return new StockAlert(listingId, userId);
    }
}
