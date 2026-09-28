package za.co.unilinkhub.saved.repository;

import za.co.unilinkhub.saved.domain.SavedListing;

import java.util.List;
import java.util.UUID;

public interface SavedListingRepository {

    SavedListing save(SavedListing savedListing);

    boolean existsByUserIdAndListingId(UUID userId, UUID listingId);

    void deleteByUserIdAndListingId(UUID userId, UUID listingId);

    List<SavedListing> findByUserId(UUID userId);

    long countByListingId(UUID listingId);
}
