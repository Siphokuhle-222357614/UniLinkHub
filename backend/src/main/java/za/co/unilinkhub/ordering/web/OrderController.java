package za.co.unilinkhub.ordering.web;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import za.co.unilinkhub.ordering.application.CartLineItem;
import za.co.unilinkhub.ordering.application.OrderDTO;
import za.co.unilinkhub.ordering.application.OrderService;
import za.co.unilinkhub.ordering.application.OrderStatsDTO;
import za.co.unilinkhub.ordering.application.SellerAnalyticsDTO;
import za.co.unilinkhub.ordering.application.SellerAnalyticsService;
import za.co.unilinkhub.security.CurrentUser;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;
    private final SellerAnalyticsService sellerAnalyticsService;

    public record ItemLine(@NotNull UUID listingId, @Positive int quantity) {
    }

    public record CheckoutRequest(@NotEmpty List<@Valid ItemLine> items, @NotBlank String fulfilmentMethod,
                                   String note, String promoCode) {
    }

    public record CancelRequest(String reason) {
    }

    @PostMapping("/api/orders")
    @ResponseStatus(HttpStatus.CREATED)
    public List<OrderDTO> checkout(@CurrentUser UUID buyerId, @Valid @RequestBody CheckoutRequest request) {
        List<CartLineItem> items = request.items().stream().map(i -> new CartLineItem(i.listingId(), i.quantity())).toList();
        return orderService.checkout(buyerId, items, request.fulfilmentMethod(), request.note(), request.promoCode());
    }

    @GetMapping("/api/orders/mine")
    public List<OrderDTO> mine(@CurrentUser UUID buyerId) {
        return orderService.listMine(buyerId);
    }

    @GetMapping("/api/orders/seller")
    public List<OrderDTO> seller(@CurrentUser UUID sellerId) {
        return orderService.listForSeller(sellerId);
    }

    @GetMapping("/api/orders/seller/analytics")
    public SellerAnalyticsDTO sellerAnalytics(@CurrentUser UUID sellerId,
                                              @RequestParam(required = false) UUID businessId,
                                              @RequestParam(defaultValue = "30") int days) {
        return sellerAnalyticsService.compute(sellerId, businessId, days);
    }

    @PostMapping("/api/orders/{id}/confirm")
    public OrderDTO confirm(@CurrentUser UUID sellerId, @PathVariable UUID id) {
        return orderService.confirm(id, sellerId);
    }

    @PostMapping("/api/orders/{id}/ready")
    public OrderDTO ready(@CurrentUser UUID sellerId, @PathVariable UUID id) {
        return orderService.markReady(id, sellerId);
    }

    @PostMapping("/api/orders/{id}/complete")
    public OrderDTO complete(@CurrentUser UUID sellerId, @PathVariable UUID id) {
        return orderService.complete(id, sellerId);
    }

    @PostMapping("/api/orders/{id}/cancel")
    public OrderDTO cancel(@CurrentUser UUID sellerId, @PathVariable UUID id, @RequestBody(required = false) CancelRequest request) {
        return orderService.cancel(id, sellerId, request == null ? null : request.reason());
    }

    @GetMapping("/api/admin/orders/stats")
    @PreAuthorize("hasRole('ADMIN')")
    public OrderStatsDTO adminStats() {
        return orderService.adminStats();
    }
}
