package za.co.unilinkhub.promo.application;

import java.math.BigDecimal;
import java.util.List;

public record PromoStatsDTO(
        long activeCount,
        long totalRedemptions,
        BigDecimal totalDiscountGiven,
        List<PromoUsageRow> topUsed
) {
    public record PromoUsageRow(String code, String businessName, String discountLabel, int usageCount, boolean active) {
    }
}
