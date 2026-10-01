package za.co.unilinkhub.listing.repository;

import za.co.unilinkhub.listing.domain.Listing;
import za.co.unilinkhub.listing.domain.ListingStatus;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import za.co.unilinkhub.shared.domain.Campus;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ListingRepository {

    Listing save(Listing listing);

    Optional<Listing> findById(UUID id);

    /**
     * Loads the listing and locks its row until the surrounding transaction ends, so two checkouts
     * buying the last item at the same moment are handled one after the other instead of both
     * seeing "1 in stock" and overselling.
     */
    Optional<Listing> findByIdForUpdate(UUID id);

    List<Listing> findAll();

    List<Listing> findByBusinessId(UUID businessId);

    List<Listing> search(String category, String keyword, BigDecimal minPrice, BigDecimal maxPrice);

    long countByStatus(ListingStatus status);

    /**
     * Live listings matching the filters, one page at a time, filtered and sorted by the database.
     * @param kind "PRODUCT", "SERVICE" or null for both
     */
    Page<Listing> searchPage(String category, String keyword, BigDecimal minPrice, BigDecimal maxPrice, String kind,
                             boolean verifiedOnly, Campus campus, Pageable pageable);

    List<CategoryCount> countActiveByCategory();
}
