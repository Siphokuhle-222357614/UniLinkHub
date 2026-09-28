package za.co.unilinkhub.ordering.application;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import za.co.unilinkhub.business.domain.Business;
import za.co.unilinkhub.business.repository.BusinessRepository;
import za.co.unilinkhub.common.exception.ResourceNotFoundException;
import za.co.unilinkhub.common.exception.UnauthorizedException;
import za.co.unilinkhub.ordering.domain.Order;
import za.co.unilinkhub.ordering.domain.OrderItem;
import za.co.unilinkhub.ordering.domain.OrderStatus;
import za.co.unilinkhub.ordering.repository.OrderRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SellerAnalyticsService {

    private static final int MIN_DAYS = 7;
    private static final int MAX_DAYS = 365;
    private static final int TOP_LISTINGS = 5;

    private final OrderRepository orderRepository;
    private final BusinessRepository businessRepository;

    /**
     * @param businessId optional - narrows the figures to one of the seller's businesses; when null
     *                   every business the seller owns is included.
     */
    public SellerAnalyticsDTO compute(UUID sellerId, UUID businessId, int requestedDays) {
        int days = Math.max(MIN_DAYS, Math.min(MAX_DAYS, requestedDays));
        List<UUID> businessIds = resolveBusinessIds(sellerId, businessId);

        LocalDate today = LocalDate.now();
        LocalDate windowStart = today.minusDays(days - 1L);
        LocalDate previousStart = windowStart.minusDays(days);

        List<Order> all = businessIds.stream().flatMap(id -> orderRepository.findByBusinessId(id).stream()).toList();
        List<Order> inWindow = between(all, windowStart, today);
        List<Order> inPrevious = between(all, previousStart, windowStart.minusDays(1));

        List<Order> counted = inWindow.stream().filter(o -> o.getStatus() != OrderStatus.CANCELLED).toList();
        List<Order> previousCounted = inPrevious.stream().filter(o -> o.getStatus() != OrderStatus.CANCELLED).toList();

        BigDecimal revenue = sumTotals(counted);
        BigDecimal averageOrderValue = counted.isEmpty()
                ? BigDecimal.ZERO
                : revenue.divide(BigDecimal.valueOf(counted.size()), 2, RoundingMode.HALF_UP);

        Map<UUID, Long> ordersPerBuyer = counted.stream()
                .collect(Collectors.groupingBy(Order::getBuyerId, Collectors.counting()));

        return new SellerAnalyticsDTO(
                days,
                revenue,
                sumTotals(previousCounted),
                counted.size(),
                previousCounted.size(),
                averageOrderValue,
                ordersPerBuyer.size(),
                ordersPerBuyer.values().stream().filter(n -> n > 1).count(),
                inWindow.size() - counted.size(),
                dailySeries(counted, windowStart, today),
                topListings(counted)
        );
    }

    private List<UUID> resolveBusinessIds(UUID sellerId, UUID businessId) {
        if (businessId == null) {
            return businessRepository.findByOwnerId(sellerId).stream().map(Business::getId).toList();
        }
        Business business = businessRepository.findById(businessId)
                .orElseThrow(() -> new ResourceNotFoundException("Business not found"));
        if (!business.getOwnerId().equals(sellerId)) {
            throw new UnauthorizedException("You do not own this business");
        }
        return List.of(businessId);
    }

    private static List<Order> between(List<Order> orders, LocalDate from, LocalDate to) {
        LocalDateTime start = from.atStartOfDay();
        LocalDateTime end = to.plusDays(1).atStartOfDay();
        return orders.stream()
                .filter(o -> o.getCreatedAt() != null && !o.getCreatedAt().isBefore(start) && o.getCreatedAt().isBefore(end))
                .toList();
    }

    private static BigDecimal sumTotals(List<Order> orders) {
        return orders.stream().map(Order::getTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static List<SellerAnalyticsDTO.DailyPoint> dailySeries(List<Order> orders, LocalDate from, LocalDate to) {
        Map<LocalDate, List<Order>> byDay = orders.stream()
                .collect(Collectors.groupingBy(o -> o.getCreatedAt().toLocalDate()));
        List<SellerAnalyticsDTO.DailyPoint> series = new ArrayList<>();
        for (LocalDate day = from; !day.isAfter(to); day = day.plusDays(1)) {
            List<Order> dayOrders = byDay.getOrDefault(day, List.of());
            series.add(new SellerAnalyticsDTO.DailyPoint(day, sumTotals(dayOrders), dayOrders.size()));
        }
        return series;
    }

    /**
     * Ranked by units sold. Revenue here is the line's list price x quantity, i.e. before any order-level
     * promo discount - a discount applies to the whole order, so it can't be split fairly per line.
     */
    private static List<SellerAnalyticsDTO.TopListing> topListings(List<Order> orders) {
        Map<UUID, String> names = new HashMap<>();
        Map<UUID, Long> units = new LinkedHashMap<>();
        Map<UUID, BigDecimal> revenue = new HashMap<>();
        for (Order order : orders) {
            for (OrderItem item : order.getItems()) {
                names.put(item.getListingId(), item.getListingName());
                units.merge(item.getListingId(), (long) item.getQuantity(), Long::sum);
                revenue.merge(item.getListingId(),
                        item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())), BigDecimal::add);
            }
        }
        return units.entrySet().stream()
                .sorted(Map.Entry.<UUID, Long>comparingByValue(Comparator.reverseOrder())
                        .thenComparing(e -> revenue.get(e.getKey()), Comparator.reverseOrder()))
                .limit(TOP_LISTINGS)
                .map(e -> new SellerAnalyticsDTO.TopListing(e.getKey(), names.get(e.getKey()), e.getValue(), revenue.get(e.getKey())))
                .toList();
    }
}
