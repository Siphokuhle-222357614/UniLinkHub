package za.co.unilinkhub.stockalert.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;
import za.co.unilinkhub.stockalert.domain.StockAlert;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface JpaStockAlertRepository extends JpaRepository<StockAlert, UUID>, za.co.unilinkhub.stockalert.repository.StockAlertRepository {
    Optional<StockAlert> findByListingIdAndUserId(UUID listingId, UUID userId);
    List<StockAlert> findByListingId(UUID listingId);
}
