package za.co.unilinkhub.ordering.application;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import za.co.unilinkhub.business.domain.Business;
import za.co.unilinkhub.business.repository.BusinessRepository;
import za.co.unilinkhub.common.exception.BadRequestException;
import za.co.unilinkhub.common.exception.ResourceNotFoundException;
import za.co.unilinkhub.listing.application.ListingService;
import za.co.unilinkhub.listing.domain.Listing;
import za.co.unilinkhub.listing.domain.ListingStatus;
import za.co.unilinkhub.listing.domain.Product;
import za.co.unilinkhub.listing.repository.ListingRepository;
import za.co.unilinkhub.notification.application.NotificationService;
import za.co.unilinkhub.ordering.domain.Order;
import za.co.unilinkhub.ordering.domain.OrderItem;
import za.co.unilinkhub.ordering.domain.factory.OrderFactory;
import za.co.unilinkhub.ordering.repository.OrderRepository;
import za.co.unilinkhub.promo.application.PromoCodeService;
import za.co.unilinkhub.user.domain.User;
import za.co.unilinkhub.user.repository.UserRepository;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Turns a buyer's cart into one Order per business - a cart spanning several sellers should
 * never become a single Order that no one seller can fully act on.
 */
@Component
@RequiredArgsConstructor
public class PlaceOrderUseCase {

    private final ListingRepository listingRepository;
    private final BusinessRepository businessRepository;
    private final ListingService listingService;
    private final PromoCodeService promoCodeService;
    private final OrderRepository orderRepository;
    private final NotificationService notificationService;
    private final UserRepository userRepository;

    private record ResolvedItem(Listing listing, int quantity) {
    }

    @Transactional
    public List<Order> execute(UUID buyerId, List<CartLineItem> cartItems, String fulfilmentMethod, String note, String promoCode) {
        if (cartItems == null || cartItems.isEmpty()) {
            throw new BadRequestException("Your cart is empty");
        }

        Map<UUID, List<ResolvedItem>> byBusiness = new LinkedHashMap<>();
        for (CartLineItem line : cartItems) {
            Listing listing = listingRepository.findById(line.listingId())
                    .orElseThrow(() -> new ResourceNotFoundException("Listing not found"));
            if (!(listing instanceof Product)) {
                throw new BadRequestException("\"" + listing.getName() + "\" can't be added to a cart - only products can");
            }
            if (listing.getStatus() != ListingStatus.ACTIVE) {
                throw new BadRequestException("\"" + listing.getName() + "\" is no longer available");
            }
            byBusiness.computeIfAbsent(listing.getBusinessId(), k -> new ArrayList<>()).add(new ResolvedItem(listing, line.quantity()));
        }

        String buyerName = userRepository.findById(buyerId).map(User::getFullName).orElse("A student");
        List<Order> created = new ArrayList<>();

        for (Map.Entry<UUID, List<ResolvedItem>> entry : byBusiness.entrySet()) {
            UUID businessId = entry.getKey();
            List<ResolvedItem> resolvedItems = entry.getValue();

            List<OrderItem> orderItems = resolvedItems.stream()
                    .map(r -> new OrderItem(r.listing().getId(), r.listing().getName(), r.listing().getPrice(), r.quantity()))
                    .toList();
            BigDecimal subtotal = orderItems.stream()
                    .map(i -> i.getUnitPrice().multiply(BigDecimal.valueOf(i.getQuantity())))
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            List<UUID> listingIds = resolvedItems.stream().map(r -> r.listing().getId()).toList();
            BigDecimal discount = promoCodeService.applyIfValid(businessId, listingIds, promoCode, subtotal);

            Order order = OrderFactory.create(buyerId, businessId, orderItems, fulfilmentMethod, note,
                    discount.signum() > 0 ? promoCode.trim().toUpperCase() : null, discount);
            created.add(orderRepository.save(order));

            for (ResolvedItem r : resolvedItems) {
                listingService.decrementStock(r.listing().getId(), r.quantity());
            }

            Business business = businessRepository.findById(businessId)
                    .orElseThrow(() -> new ResourceNotFoundException("Business not found"));
            notificationService.notify(business.getOwnerId(), "ORDER",
                    "New order from " + buyerName + " (" + orderItems.size() + " item" + (orderItems.size() == 1 ? "" : "s") + ")");
        }

        return created;
    }
}
