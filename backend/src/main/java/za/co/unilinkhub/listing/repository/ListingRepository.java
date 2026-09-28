package za.co.unilinkhub.listing.repository;

import za.co.unilinkhub.listing.domain.Listing;
import za.co.unilinkhub.listing.domain.ListingStatus;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ListingRepository {

    Listing save(Listing listing);

    Optional<Listing> findById(UUID id);

    List<Listing> findByBusinessId(UUID businessId);

    List<Listing> search(String category, String keyword, BigDecimal minPrice, BigDecimal maxPrice);

    long countByStatus(ListingStatus status);
}
