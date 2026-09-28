package za.co.unilinkhub.promo.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * A seller-defined discount code, scoped to one business and optionally to a single listing
 * within it. Usable while active and not expired; usage is tracked so the seller and admin
 * can see how much it has actually been redeemed for.
 */
@Entity
@Table(name = "promo_codes", uniqueConstraints = {
        @UniqueConstraint(name = "uk_promo_codes_business_code", columnNames = {"business_id", "code"})
})
@Getter
@NoArgsConstructor
public class PromoCode {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "business_id", nullable = false)
    private UUID businessId;

    @Column(nullable = false, length = 40)
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(name = "discount_type", nullable = false, length = 20)
    private DiscountType discountType;

    @Column(name = "discount_value", nullable = false, precision = 10, scale = 2)
    private BigDecimal discountValue;

    @Column(name = "scope_listing_id")
    private UUID scopeListingId;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "usage_count", nullable = false)
    private int usageCount = 0;

    @Column(name = "total_discount_given", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalDiscountGiven = BigDecimal.ZERO;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private PromoCode(UUID businessId, String code, DiscountType discountType, BigDecimal discountValue,
                       UUID scopeListingId, LocalDateTime expiresAt) {
        this.businessId = businessId;
        this.code = code;
        this.discountType = discountType;
        this.discountValue = discountValue;
        this.scopeListingId = scopeListingId;
        this.expiresAt = expiresAt;
    }

    public static PromoCode create(UUID businessId, String code, DiscountType discountType, BigDecimal discountValue,
                                    UUID scopeListingId, LocalDateTime expiresAt) {
        return new PromoCode(businessId, code, discountType, discountValue, scopeListingId, expiresAt);
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public boolean isUsable() {
        return active && (expiresAt == null || expiresAt.isAfter(LocalDateTime.now()));
    }

    public BigDecimal computeDiscount(BigDecimal subtotal) {
        if (discountType == DiscountType.PERCENT) {
            return subtotal.multiply(discountValue).divide(BigDecimal.valueOf(100));
        }
        return discountValue.min(subtotal);
    }

    public void recordUsage(BigDecimal discountAmount) {
        this.usageCount++;
        this.totalDiscountGiven = this.totalDiscountGiven.add(discountAmount);
    }
}
