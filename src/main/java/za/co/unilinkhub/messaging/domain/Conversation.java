package za.co.unilinkhub.messaging.domain;

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
 * A single message thread between one buyer and one business. There is at most one conversation
 * per (business, buyer) pair - starting a new conversation from a different listing on the same
 * business continues the existing thread rather than fragmenting it, the way a real inbox would.
 */
@Entity
@Table(name = "conversations", uniqueConstraints = {
        @UniqueConstraint(name = "uk_conversations_business_buyer", columnNames = {"business_id", "buyer_id"})
})
@Getter
@NoArgsConstructor
public class Conversation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "business_id", nullable = false)
    private UUID businessId;

    @Column(name = "buyer_id", nullable = false)
    private UUID buyerId;

    @Column(name = "seller_id", nullable = false)
    private UUID sellerId;

    @Column(name = "listing_id")
    private UUID listingId;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private Conversation(UUID businessId, UUID buyerId, UUID sellerId, UUID listingId) {
        this.businessId = businessId;
        this.buyerId = buyerId;
        this.sellerId = sellerId;
        this.listingId = listingId;
    }

    public static Conversation start(UUID businessId, UUID buyerId, UUID sellerId, UUID listingId) {
        return new Conversation(businessId, buyerId, sellerId, listingId);
    }
}
