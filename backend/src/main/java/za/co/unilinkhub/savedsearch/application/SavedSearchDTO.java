package za.co.unilinkhub.savedsearch.application;

import za.co.unilinkhub.savedsearch.domain.SavedSearch;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record SavedSearchDTO(
        UUID id,
        String label,
        String keyword,
        String category,
        BigDecimal maxPrice,
        String listingType,
        boolean alertsEnabled,
        long newMatchesCount,
        LocalDateTime createdAt
) {
    public static SavedSearchDTO from(SavedSearch savedSearch, long newMatchesCount) {
        return new SavedSearchDTO(savedSearch.getId(), savedSearch.getLabel(), savedSearch.getKeyword(),
                savedSearch.getCategory(), savedSearch.getMaxPrice(), savedSearch.getListingType(),
                savedSearch.isAlertsEnabled(), newMatchesCount, savedSearch.getCreatedAt());
    }
}
