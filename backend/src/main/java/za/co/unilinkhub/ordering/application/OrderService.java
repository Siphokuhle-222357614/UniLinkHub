package za.co.unilinkhub.ordering.application;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import za.co.unilinkhub.business.domain.Business;
import za.co.unilinkhub.business.repository.BusinessRepository;
import za.co.unilinkhub.common.exception.BadRequestException;
import za.co.unilinkhub.common.exception.ForbiddenException;
import za.co.unilinkhub.common.exception.ResourceNotFoundException;
import za.co.unilinkhub.common.exception.TooManyRequestsException;
import za.co.unilinkhub.listing.application.ListingService;
import za.co.unilinkhub.notification.application.NotificationService;
import za.co.unilinkhub.ordering.domain.Order;
import za.co.unilinkhub.ordering.domain.OrderItem;
import za.co.unilinkhub.ordering.domain.OrderStatus;
import za.co.unilinkhub.ordering.repository.OrderRepository;
import za.co.unilinkhub.user.domain.User;
import za.co.unilinkhub.user.repository.UserRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final PlaceOrderUseCase placeOrderUseCase;
    private final BusinessRepository businessRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final ListingService listingService;

    // Deliberately NOT @Transactional here: PlaceOrderUseCase.execute() already commits its own
    // transaction (and flushes each Order's @CreationTimestamp) before returning. Wrapping this
    // method too would extend that transaction across the DTO-mapping reads below, deferring the
    // flush until this method returns and leaving createdAt null on the response.
    public List<OrderDTO> checkout(UUID buyerId, List<CartLineItem> items, String fulfilmentMethod, String note, String promoCode) {
        return placeOrderUseCase.execute(buyerId, items, fulfilmentMethod, note, promoCode).stream().map(this::toBuyerDTO).toList();
    }

    public List<OrderDTO> listMine(UUID buyerId) {
        return orderRepository.findByBuyerId(buyerId).stream()
                .sorted(Comparator.comparing(Order::getCreatedAt).reversed())
                .map(this::toBuyerDTO)
                .toList();
    }

    public List<OrderDTO> listForSeller(UUID sellerId) {
        List<UUID> businessIds = businessRepository.findByOwnerId(sellerId).stream().map(Business::getId).toList();
        return businessIds.stream()
                .flatMap(id -> orderRepository.findByBusinessId(id).stream())
                .sorted(Comparator.comparing(Order::getCreatedAt).reversed())
                .map(this::toDTO)
                .toList();
    }

    public OrderDTO confirm(UUID orderId, UUID sellerId) {
        Order order = findOwned(orderId, sellerId);
        order.confirm();
        Order saved = orderRepository.save(order);
        notifyBuyer(saved, "confirmed");
        return toDTO(saved);
    }

    public OrderDTO markReady(UUID orderId, UUID sellerId) {
        Order order = findOwned(orderId, sellerId);
        order.markReady();
        Order saved = orderRepository.save(order);
        notifyBuyer(saved, saved.getPickupCode() != null
                ? "ready for pickup. Show your pickup code " + saved.getPickupCode() + " when you collect it"
                : "ready for pickup");
        return toDTO(saved);
    }

    /**
     * Deliberately not @Transactional: a wrong pickup code is saved (to count towards the lock-out)
     * before the rejection is thrown, and a transaction would roll that count back.
     */
    public OrderDTO complete(UUID orderId, UUID sellerId, String pickupCode) {
        Order order = findOwned(orderId, sellerId);
        if (order.getStatus() == OrderStatus.READY) {
            switch (order.checkPickupCode(pickupCode, LocalDateTime.now())) {
                case LOCKED -> throw new TooManyRequestsException("Too many wrong pickup codes for this order. Please wait 15 minutes, "
                        + "then ask the buyer to show you the code on their My orders page again.");
                case WRONG -> {
                    orderRepository.save(order);
                    int left = order.pickupCodeTriesLeft();
                    throw new BadRequestException((pickupCode == null || pickupCode.isBlank()
                            ? "Please enter the buyer's 4-digit pickup code."
                            : "That pickup code doesn't match.")
                            + " Ask the buyer to open My orders and show you their code - it proves they received the order. "
                            + (left > 0 ? left + " tr" + (left == 1 ? "y" : "ies") + " left." : "Completing this order is now paused for 15 minutes."));
                }
                case OK -> { }
            }
        }
        order.complete();
        Order saved = orderRepository.save(order);
        notifyBuyer(saved, "completed");
        return toDTO(saved);
    }

    // One transaction, so the order is never left cancelled with its stock not yet returned.
    @Transactional
    public OrderDTO cancel(UUID orderId, UUID sellerId, String reason) {
        Order order = findOwned(orderId, sellerId);
        order.cancel(reason);
        Order saved = orderRepository.save(order);
        for (OrderItem item : saved.getItems()) {
            listingService.restoreStock(item.getListingId(), item.getQuantity());
        }
        notifyBuyer(saved, "cancelled" + (reason != null && !reason.isBlank() ? ": " + reason : ""));
        return toDTO(saved);
    }

    public OrderStatsDTO adminStats() {
        List<Order> all = orderRepository.findAll();
        BigDecimal gross = all.stream()
                .filter(o -> o.getStatus() != OrderStatus.CANCELLED)
                .map(Order::getTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        long placed = all.stream().filter(o -> o.getStatus() == OrderStatus.PLACED).count();
        long confirmed = all.stream().filter(o -> o.getStatus() == OrderStatus.CONFIRMED).count();
        long ready = all.stream().filter(o -> o.getStatus() == OrderStatus.READY).count();
        long completed = all.stream().filter(o -> o.getStatus() == OrderStatus.COMPLETED).count();
        long cancelled = all.stream().filter(o -> o.getStatus() == OrderStatus.CANCELLED).count();

        List<OrderDTO> recent = all.stream()
                .sorted(Comparator.comparing(Order::getCreatedAt).reversed())
                .limit(10)
                .map(this::toDTO)
                .toList();

        return new OrderStatsDTO(all.size(), gross, placed, confirmed, ready, completed, cancelled, recent);
    }

    private void notifyBuyer(Order order, String outcome) {
        String businessName = businessRepository.findById(order.getBusinessId()).map(Business::getBusinessName).orElse("The seller");
        notificationService.notify(order.getBuyerId(), "ORDER", "Your order from " + businessName + " was " + outcome);
    }

    private Order findOwned(UUID orderId, UUID sellerId) {
        Order order = orderRepository.findById(orderId).orElseThrow(() -> new ResourceNotFoundException("We couldn't find that order."));
        Business business = businessRepository.findById(order.getBusinessId())
                .orElseThrow(() -> new ResourceNotFoundException("We couldn't find that business. It may have been removed."));
        if (!business.getOwnerId().equals(sellerId)) {
            throw new ForbiddenException("Only the owner of this business can do this. You can only manage businesses you created yourself.");
        }
        return order;
    }

    /** Seller and admin views: never include the pickup code. */
    private OrderDTO toDTO(Order order) {
        return toDTO(order, false);
    }

    /** The buyer's own view: includes the pickup code they show at handover. */
    private OrderDTO toBuyerDTO(Order order) {
        return toDTO(order, true);
    }

    private OrderDTO toDTO(Order order, boolean forBuyer) {
        String buyerName = userRepository.findById(order.getBuyerId()).map(User::getFullName).orElse("Deleted account");
        String businessName = businessRepository.findById(order.getBusinessId()).map(Business::getBusinessName).orElse("Unknown business");
        return OrderDTO.from(order, buyerName, businessName, forBuyer);
    }
}
