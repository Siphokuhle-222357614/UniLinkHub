package za.co.unilinkhub.listing.application;

import za.co.unilinkhub.listing.domain.Listing;
import za.co.unilinkhub.listing.domain.Product;
import za.co.unilinkhub.listing.domain.Service;
import za.co.unilinkhub.trust.application.SellerTrust;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * A listing as the frontend sees it. The seller fields ({@code businessName}, {@code campus},
 * {@code seller}...) are filled in by {@link ListingEnricher} for pages that show them - a bare
 * {@link #from(Listing)} leaves them null.
 */
public record ListingDTO(
        UUID id,
        UUID businessId,
        String type,
        String name,
        String description,
        String category,
        BigDecimal price,
        String status,
        long viewCount,
        Integer stockQuantity,
        String imageUrl,
        List<String> imageUrls,
        Integer lowStockThreshold,
        Integer durationMinutes,
        String availabilitySchedule,
        LocalDateTime createdAt,
        long savedCount,
        String takedownReason,
        LocalDateTime takenDownAt,
        String businessName,
        String campus,
        String campusLabel,
        String pickupLocation,
        SellerTrust seller
) {
    public ListingDTO withSavedCount(long savedCount) {
        return new ListingDTO(id, businessId, type, name, description, category, price, status, viewCount, stockQuantity,
                imageUrl, imageUrls, lowStockThreshold, durationMinutes, availabilitySchedule, createdAt, savedCount,
                takedownReason, takenDownAt, businessName, campus, campusLabel, pickupLocation, seller);
    }

    public ListingDTO withSeller(String businessName, String campus, String campusLabel, String pickupLocation, SellerTrust seller) {
        return new ListingDTO(id, businessId, type, name, description, category, price, status, viewCount, stockQuantity,
                imageUrl, imageUrls, lowStockThreshold, durationMinutes, availabilitySchedule, createdAt, savedCount,
                takedownReason, takenDownAt, businessName, campus, campusLabel, pickupLocation, seller);
    }

    public static ListingDTO from(Listing listing) {
        Integer stockQuantity = null;
        Integer lowStockThreshold = null;
        Integer durationMinutes = null;
        String availabilitySchedule = null;
        String type;

        if (listing instanceof Product product) {
            type = "PRODUCT";
            stockQuantity = product.getStockQuantity();
            lowStockThreshold = product.getLowStockThreshold();
        } else if (listing instanceof Service service) {
            type = "SERVICE";
            durationMinutes = service.getDurationMinutes();
            availabilitySchedule = service.getAvailabilitySchedule();
        } else {
            type = "UNKNOWN";
        }

        return new ListingDTO(
                listing.getId(), listing.getBusinessId(), type, listing.getName(),
                listing.getDescription(), listing.getCategory(), listing.getPrice(),
                listing.getStatus().name(), listing.getViewCount(),
                stockQuantity, listing.getImageUrl(), listing.getGallery(), lowStockThreshold, durationMinutes,
                availabilitySchedule, listing.getCreatedAt(), 0, listing.getTakedownReason(), listing.getTakenDownAt(),
                null, null, null, null, null
        );
    }
}
