package za.co.unilinkhub.listing.infrastructure;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import za.co.unilinkhub.listing.repository.CategoryCount;
import za.co.unilinkhub.shared.domain.Campus;
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
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT l FROM Listing l WHERE l.id = :id")
    Optional<Listing> findByIdForUpdate(@Param("id") UUID id);

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

    @Override
    @Query(value = """
            SELECT l FROM Listing l
            WHERE l.status = za.co.unilinkhub.listing.domain.ListingStatus.ACTIVE
              AND (:category IS NULL OR LOWER(l.category) = LOWER(:category))
              AND (:keyword IS NULL
                   OR LOWER(l.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
                   OR LOWER(l.description) LIKE LOWER(CONCAT('%', :keyword, '%')))
              AND (:minPrice IS NULL OR l.price >= :minPrice)
              AND (:maxPrice IS NULL OR l.price <= :maxPrice)
              AND (:kind IS NULL OR (:kind = 'PRODUCT' AND TYPE(l) = Product) OR (:kind = 'SERVICE' AND TYPE(l) = Service))
              AND (:campus IS NULL OR l.businessId IN (SELECT b.id FROM Business b WHERE b.campus = :campus))
              AND (:verifiedOnly = false OR l.businessId IN (
                   SELECT v.id FROM Business v WHERE v.verificationStatus = za.co.unilinkhub.business.domain.VerificationStatus.VERIFIED))
            """, countQuery = """
            SELECT COUNT(l) FROM Listing l
            WHERE l.status = za.co.unilinkhub.listing.domain.ListingStatus.ACTIVE
              AND (:category IS NULL OR LOWER(l.category) = LOWER(:category))
              AND (:keyword IS NULL
                   OR LOWER(l.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
                   OR LOWER(l.description) LIKE LOWER(CONCAT('%', :keyword, '%')))
              AND (:minPrice IS NULL OR l.price >= :minPrice)
              AND (:maxPrice IS NULL OR l.price <= :maxPrice)
              AND (:kind IS NULL OR (:kind = 'PRODUCT' AND TYPE(l) = Product) OR (:kind = 'SERVICE' AND TYPE(l) = Service))
              AND (:campus IS NULL OR l.businessId IN (SELECT b.id FROM Business b WHERE b.campus = :campus))
              AND (:verifiedOnly = false OR l.businessId IN (
                   SELECT v.id FROM Business v WHERE v.verificationStatus = za.co.unilinkhub.business.domain.VerificationStatus.VERIFIED))
            """)
    Page<Listing> searchPage(@Param("category") String category, @Param("keyword") String keyword,
                             @Param("minPrice") BigDecimal minPrice, @Param("maxPrice") BigDecimal maxPrice,
                             @Param("kind") String kind, @Param("verifiedOnly") boolean verifiedOnly,
                             @Param("campus") Campus campus, Pageable pageable);

    @Override
    @Query("""
            SELECT new za.co.unilinkhub.listing.repository.CategoryCount(l.category, COUNT(l))
            FROM Listing l WHERE l.status = za.co.unilinkhub.listing.domain.ListingStatus.ACTIVE
            GROUP BY l.category
            """)
    List<CategoryCount> countActiveByCategory();
}
