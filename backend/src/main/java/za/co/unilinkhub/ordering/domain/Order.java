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
import java.security.SecureRandom;
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

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int MAX_PICKUP_CODE_FAILURES = 5;

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

    /**
     * Shown only to the buyer; the seller enters it to complete the order, which proves the handover
     * happened. Null on orders placed before pickup codes existed - those complete without one.
     */
    @Column(name = "pickup_code", length = 4)
    private String pickupCode;

    @Column(name = "pickup_code_failures", nullable = false)
    private int pickupCodeFailures = 0;

    @Column(name = "pickup_code_locked_until")
    private LocalDateTime pickupCodeLockedUntil;

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
        this.pickupCode = String.format("%04d", RANDOM.nextInt(10_000));
    }

    public static Order create(UUID buyerId, UUID businessId, List<OrderItem> items, String fulfilmentMethod,
                                String note, String promoCode, BigDecimal subtotal, BigDecimal discountAmount,
                                BigDecimal total) {
        return new Order(buyerId, businessId, items, fulfilmentMethod, note, promoCode, subtotal, discountAmount, total);
    }

    public void confirm() {
        if (status != OrderStatus.PLACED) {
            throw new IllegalStateException("This order can't be confirmed because it isn't waiting for confirmation any more (it may already be confirmed or cancelled).");
        }
        this.status = OrderStatus.CONFIRMED;
    }

    public void markReady() {
        if (status != OrderStatus.CONFIRMED) {
            throw new IllegalStateException("Please confirm this order before marking it as ready for pickup.");
        }
        this.status = OrderStatus.READY;
    }

    public enum PickupCheck { OK, WRONG, LOCKED }

    /**
     * Checks the code the seller typed in. Five wrong tries lock completion for 15 minutes, so the
     * 10,000 possible codes can't simply be guessed.
     */
    public PickupCheck checkPickupCode(String entered, LocalDateTime now) {
        if (pickupCode == null) {
            return PickupCheck.OK;
        }
        if (pickupCodeLockedUntil != null && now.isBefore(pickupCodeLockedUntil)) {
            return PickupCheck.LOCKED;
        }
        if (pickupCodeLockedUntil != null) {
            pickupCodeLockedUntil = null;
            pickupCodeFailures = 0;
        }
        if (entered != null && pickupCode.equals(entered.trim())) {
            return PickupCheck.OK;
        }
        pickupCodeFailures++;
        if (pickupCodeFailures >= MAX_PICKUP_CODE_FAILURES) {
            pickupCodeLockedUntil = now.plusMinutes(15);
        }
        return PickupCheck.WRONG;
    }

    public int pickupCodeTriesLeft() {
        return Math.max(0, MAX_PICKUP_CODE_FAILURES - pickupCodeFailures);
    }

    public void complete() {
        if (status != OrderStatus.READY) {
            throw new IllegalStateException("Please mark this order as ready for pickup before completing it.");
        }
        this.status = OrderStatus.COMPLETED;
    }

    public void cancel(String reason) {
        if (status == OrderStatus.COMPLETED || status == OrderStatus.CANCELLED) {
            throw new IllegalStateException("This order can't be cancelled because it's already completed or cancelled.");
        }
        this.status = OrderStatus.CANCELLED;
        this.cancelReason = reason;
    }
}
