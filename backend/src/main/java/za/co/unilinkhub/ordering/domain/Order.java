package za.co.unilinkhub.ordering.domain;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * One business's slice of a checkout. A cart spanning several businesses becomes one Order per
 * business (see PlaceOrderUseCase) so each seller only ever sees and manages their own orders.
 * There is no online payment - fulfilment and payment are arranged directly between buyer and
 * seller on pickup/delivery, consistent with the rest of this MVP.
 */
@Entity
@Table(name = "orders")
@Getter
@NoArgsConstructor
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "buyer_id", nullable = false)
    private UUID buyerId;

    @Column(name = "business_id", nullable = false)
    private UUID businessId;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "order_items", joinColumns = @JoinColumn(name = "order_id"))
    private List<OrderItem> items = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OrderStatus status = OrderStatus.PLACED;

    @Column(name = "fulfilment_method", nullable = false, length = 30)
    private String fulfilmentMethod;

    @Column(length = 1000)
    private String note;

    @Column(name = "promo_code", length = 40)
    private String promoCode;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal subtotal;

    @Column(name = "discount_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal discountAmount;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal total;

    @Column(name = "cancel_reason", length = 1000)
    private String cancelReason;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    private Order(UUID buyerId, UUID businessId, List<OrderItem> items, String fulfilmentMethod, String note,
                   String promoCode, BigDecimal subtotal, BigDecimal discountAmount, BigDecimal total) {
        this.buyerId = buyerId;
        this.businessId = businessId;
        this.items = items;
        this.fulfilmentMethod = fulfilmentMethod;
        this.note = note;
        this.promoCode = promoCode;
        this.subtotal = subtotal;
        this.discountAmount = discountAmount;
        this.total = total;
    }

    public static Order create(UUID buyerId, UUID businessId, List<OrderItem> items, String fulfilmentMethod,
                                String note, String promoCode, BigDecimal subtotal, BigDecimal discountAmount,
                                BigDecimal total) {
        return new Order(buyerId, businessId, items, fulfilmentMethod, note, promoCode, subtotal, discountAmount, total);
    }

    public void confirm() {
        if (status != OrderStatus.PLACED) {
            throw new IllegalStateException("Order is not awaiting confirmation");
        }
        this.status = OrderStatus.CONFIRMED;
    }

    public void markReady() {
        if (status != OrderStatus.CONFIRMED) {
            throw new IllegalStateException("Order must be confirmed before it can be marked ready");
        }
        this.status = OrderStatus.READY;
    }

    public void complete() {
        if (status != OrderStatus.READY) {
            throw new IllegalStateException("Order must be ready before it can be completed");
        }
        this.status = OrderStatus.COMPLETED;
    }

    public void cancel(String reason) {
        if (status == OrderStatus.COMPLETED || status == OrderStatus.CANCELLED) {
            throw new IllegalStateException("Order can no longer be cancelled");
        }
        this.status = OrderStatus.CANCELLED;
        this.cancelReason = reason;
    }
}
