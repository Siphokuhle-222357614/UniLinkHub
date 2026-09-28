package za.co.unilinkhub.stockalert.repository;

import za.co.unilinkhub.stockalert.domain.StockAlert;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface StockAlertRepository {
    StockAlert save(StockAlert alert);
    Optional<StockAlert> findByListingIdAndUserId(UUID listingId, UUID userId);
    List<StockAlert> findByListingId(UUID listingId);
    void delete(StockAlert alert);
    void deleteAll(Iterable<? extends StockAlert> alerts);
}
