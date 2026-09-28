package za.co.unilinkhub.stockalert.application;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import za.co.unilinkhub.notification.application.NotificationService;
import za.co.unilinkhub.stockalert.domain.StockAlert;
import za.co.unilinkhub.stockalert.repository.StockAlertRepository;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class StockAlertService {

    private final StockAlertRepository stockAlertRepository;
    private final NotificationService notificationService;

    public boolean toggle(UUID userId, UUID listingId) {
        return stockAlertRepository.findByListingIdAndUserId(listingId, userId)
                .map(existing -> {
                    stockAlertRepository.delete(existing);
                    return false;
                })
                .orElseGet(() -> {
                    stockAlertRepository.save(StockAlert.subscribe(listingId, userId));
                    return true;
                });
    }

    public boolean isSubscribed(UUID userId, UUID listingId) {
        return stockAlertRepository.findByListingIdAndUserId(listingId, userId).isPresent();
    }

    public void notifyAndClear(UUID listingId, String listingName) {
        List<StockAlert> alerts = stockAlertRepository.findByListingId(listingId);
        for (StockAlert alert : alerts) {
            notificationService.notify(alert.getUserId(), "STOCK", "\"" + listingName + "\" is back in stock");
        }
        stockAlertRepository.deleteAll(alerts);
    }
}
