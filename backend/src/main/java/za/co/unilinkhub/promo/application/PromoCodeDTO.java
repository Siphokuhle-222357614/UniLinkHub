package za.co.unilinkhub.promo.application;

import za.co.unilinkhub.promo.domain.PromoCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record PromoCodeDTO(
        UUID id,
        UUID businessId,
        String code,
        String discountType,
        BigDecimal discountValue,
        UUID scopeListingId,
        String scopeListingName,
        LocalDateTime expiresAt,
        boolean active,
        boolean usable,
        int usageCount,
        LocalDateTime createdAt
) {
    public static PromoCodeDTO from(PromoCode promoCode, String scopeListingName) {
        return new PromoCodeDTO(
                promoCode.getId(), promoCode.getBusinessId(), promoCode.getCode(),
                promoCode.getDiscountType().name(), promoCode.getDiscountValue(), promoCode.getScopeListingId(),
                scopeListingName, promoCode.getExpiresAt(), promoCode.isActive(), promoCode.isUsable(),
                promoCode.getUsageCount(), promoCode.getCreatedAt()
        );
    }
}
