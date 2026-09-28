package za.co.unilinkhub.booking.application;

import java.util.List;

public record BookingStatsDTO(
        long total,
        long pending,
        long accepted,
        long declined,
        List<TopService> mostBooked
) {
    public record TopService(String listingName, String businessName, long count) {
    }
}
