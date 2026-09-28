package za.co.unilinkhub.booking.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * A buyer's request to book a Service listing at a preferred date/time. There's no calendar or
 * conflict detection - the seller just accepts or declines, same as a direct message would work,
 * but tracked so both sides can see the status.
 */
@Entity
@Table(name = "bookings")
@Getter
@NoArgsConstructor
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "listing_id", nullable = false)
    private UUID listingId;

    @Column(name = "business_id", nullable = false)
    private UUID businessId;

    @Column(name = "buyer_id", nullable = false)
    private UUID buyerId;

    @Column(name = "preferred_at", nullable = false)
    private LocalDateTime preferredAt;

    @Column(length = 1000)
    private String note;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BookingStatus status = BookingStatus.PENDING;

    @Column(name = "decline_reason", length = 1000)
    private String declineReason;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private Booking(UUID listingId, UUID businessId, UUID buyerId, LocalDateTime preferredAt, String note) {
        this.listingId = listingId;
        this.businessId = businessId;
        this.buyerId = buyerId;
        this.preferredAt = preferredAt;
        this.note = note;
    }

    public static Booking request(UUID listingId, UUID businessId, UUID buyerId, LocalDateTime preferredAt, String note) {
        return new Booking(listingId, businessId, buyerId, preferredAt, note);
    }

    public void accept() {
        this.status = BookingStatus.ACCEPTED;
        this.declineReason = null;
    }

    public void decline(String reason) {
        this.status = BookingStatus.DECLINED;
        this.declineReason = reason;
    }
}
