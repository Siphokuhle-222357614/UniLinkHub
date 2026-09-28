package za.co.unilinkhub.booking.application;

import za.co.unilinkhub.booking.domain.Booking;

import java.time.LocalDateTime;
import java.util.UUID;

public record BookingSummaryView(
        UUID id,
        UUID listingId,
        String listingName,
        UUID businessId,
        String businessName,
        UUID buyerId,
        String buyerName,
        LocalDateTime preferredAt,
        String note,
        String status,
        String declineReason,
        LocalDateTime createdAt
) {
    public static BookingSummaryView of(Booking booking, String listingName, String businessName, String buyerName) {
        return new BookingSummaryView(
                booking.getId(), booking.getListingId(), listingName, booking.getBusinessId(), businessName,
                booking.getBuyerId(), buyerName, booking.getPreferredAt(), booking.getNote(),
                booking.getStatus().name(), booking.getDeclineReason(), booking.getCreatedAt()
        );
    }
}
