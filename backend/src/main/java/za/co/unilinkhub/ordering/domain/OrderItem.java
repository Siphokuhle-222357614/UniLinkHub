package za.co.unilinkhub.ordering.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * A snapshot of one listing's name and price at the moment an order was placed, so later
 * price or name changes on the listing never alter the record of what was actually ordered.
 */
@Embeddable
@Getter
@NoArgsConstructor
public class OrderItem {

    @Column(name = "listing_id", nullable = false)
    private UUID listingId;

    @Column(name = "listing_name", nullable = false, length = 150)
    private String listingName;

    @Column(name = "unit_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "quantity", nullable = false)
    private int quantity;

    public OrderItem(UUID listingId, String listingName, BigDecimal unitPrice, int quantity) {
        this.listingId = listingId;
        this.listingName = listingName;
        this.unitPrice = unitPrice;
        this.quantity = quantity;
    }
}
