package za.co.unilinkhub.saved.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;
import za.co.unilinkhub.saved.domain.SavedListing;
import za.co.unilinkhub.saved.repository.SavedListingRepository;

import java.util.List;
import java.util.UUID;

public interface JpaSavedListingRepository extends JpaRepository<SavedListing, UUID>, SavedListingRepository {

    @Override
    boolean existsByUserIdAndListingId(UUID userId, UUID listingId);

    @Override
    void deleteByUserIdAndListingId(UUID userId, UUID listingId);

    @Override
    List<SavedListing> findByUserId(UUID userId);

    @Override
    long countByListingId(UUID listingId);
}
