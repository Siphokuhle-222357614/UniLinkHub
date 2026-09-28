package za.co.unilinkhub.listing.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import za.co.unilinkhub.listing.domain.Listing;
import za.co.unilinkhub.listing.domain.ListingStatus;
import za.co.unilinkhub.listing.repository.ListingRepository;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface JpaListingRepository extends JpaRepository<Listing, UUID>, ListingRepository {

    @Override
    List<Listing> findByBusinessId(UUID businessId);

    @Override
    @Query("""
            SELECT l FROM Listing l
            WHERE l.status = za.co.unilinkhub.listing.domain.ListingStatus.ACTIVE
              AND (:category IS NULL OR LOWER(l.category) = LOWER(:category))
              AND (:keyword IS NULL
                   OR LOWER(l.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
                   OR LOWER(l.description) LIKE LOWER(CONCAT('%', :keyword, '%')))
              AND (:minPrice IS NULL OR l.price >= :minPrice)
              AND (:maxPrice IS NULL OR l.price <= :maxPrice)
            ORDER BY l.createdAt DESC
            """)
    List<Listing> search(@Param("category") String category, @Param("keyword") String keyword,
                          @Param("minPrice") BigDecimal minPrice, @Param("maxPrice") BigDecimal maxPrice);

    @Override
    long countByStatus(ListingStatus status);
}
