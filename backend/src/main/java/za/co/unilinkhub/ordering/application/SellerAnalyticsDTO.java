package za.co.unilinkhub.ordering.application;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * A seller's sales performance over a rolling window of {@code days}, plus the same figures for the
 * window immediately before it so the UI can show a trend ("+12% vs previous 30 days"). Cancelled
 * orders never count towards revenue, order counts or buyers - they only show up in
 * {@code cancelledOrders}.
 */
public record SellerAnalyticsDTO(
        int days,
        BigDecimal revenue,
        BigDecimal previousRevenue,
        long orders,
        long previousOrders,
        BigDecimal averageOrderValue,
        long uniqueBuyers,
        long repeatBuyers,
        long cancelledOrders,
        List<DailyPoint> daily,
        List<TopListing> topListings
) {

    /** One calendar day in the window; days with no orders are included with zeros so charts don't skip gaps. */
    public record DailyPoint(LocalDate date, BigDecimal revenue, long orders) {
    }

    public record TopListing(UUID listingId, String name, long unitsSold, BigDecimal revenue) {
    }
}
