package za.co.unilinkhub.listing.application;

import za.co.unilinkhub.listing.domain.Listing;
import za.co.unilinkhub.listing.domain.Product;
import za.co.unilinkhub.listing.domain.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

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
        Integer lowStockThreshold,
        Integer durationMinutes,
        String availabilitySchedule,
        LocalDateTime createdAt,
        long savedCount
) {
    public ListingDTO withSavedCount(long savedCount) {
        return new ListingDTO(id, businessId, type, name, description, category, price, status, viewCount,
                stockQuantity, imageUrl, lowStockThreshold, durationMinutes, availabilitySchedule, createdAt, savedCount);
    }

    public static ListingDTO from(Listing listing) {
        Integer stockQuantity = null;
        String imageUrl = null;
        Integer lowStockThreshold = null;
        Integer durationMinutes = null;
        String availabilitySchedule = null;
        String type;

        if (listing instanceof Product product) {
            type = "PRODUCT";
            stockQuantity = product.getStockQuantity();
            imageUrl = product.getImageUrl();
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
                stockQuantity, imageUrl, lowStockThreshold, durationMinutes, availabilitySchedule,
                listing.getCreatedAt(), 0
        );
    }
}
