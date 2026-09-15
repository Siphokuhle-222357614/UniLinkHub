package za.co.unilinkhub.ordering.application;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import za.co.unilinkhub.business.domain.Business;
import za.co.unilinkhub.business.repository.BusinessRepository;
import za.co.unilinkhub.common.exception.ResourceNotFoundException;
import za.co.unilinkhub.common.exception.UnauthorizedException;
import za.co.unilinkhub.notification.application.NotificationService;
import za.co.unilinkhub.ordering.domain.Order;
import za.co.unilinkhub.ordering.domain.OrderStatus;
import za.co.unilinkhub.ordering.repository.OrderRepository;
import za.co.unilinkhub.user.domain.User;
import za.co.unilinkhub.user.repository.UserRepository;

import java.math.BigDecimal;
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

    // Deliberately NOT @Transactional here: PlaceOrderUseCase.execute() already commits its own
    // transaction (and flushes each Order's @CreationTimestamp) before returning. Wrapping this
    // method too would extend that transaction across the DTO-mapping reads below, deferring the
    // flush until this method returns and leaving createdAt null on the response.
    public List<OrderDTO> checkout(UUID buyerId, List<CartLineItem> items, String fulfilmentMethod, String note, String promoCode) {
        return placeOrderUseCase.execute(buyerId, items, fulfilmentMethod, note, promoCode).stream().map(this::toDTO).toList();
    }

    public List<OrderDTO> listMine(UUID buyerId) {
        return orderRepository.findByBuyerId(buyerId).stream()
                .sorted(Comparator.comparing(Order::getCreatedAt).reversed())
                .map(this::toDTO)
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
        notifyBuyer(saved, "ready for pickup");
        return toDTO(saved);
    }

    public OrderDTO complete(UUID orderId, UUID sellerId) {
        Order order = findOwned(orderId, sellerId);
        order.complete();
        Order saved = orderRepository.save(order);
        notifyBuyer(saved, "completed");
        return toDTO(saved);
    }

    public OrderDTO cancel(UUID orderId, UUID sellerId, String reason) {
        Order order = findOwned(orderId, sellerId);
        order.cancel(reason);
        Order saved = orderRepository.save(order);
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
        Order order = orderRepository.findById(orderId).orElseThrow(() -> new ResourceNotFoundException("Order not found"));
        Business business = businessRepository.findById(order.getBusinessId())
                .orElseThrow(() -> new ResourceNotFoundException("Business not found"));
        if (!business.getOwnerId().equals(sellerId)) {
            throw new UnauthorizedException("You do not own this business");
        }
        return order;
    }

    private OrderDTO toDTO(Order order) {
        String buyerName = userRepository.findById(order.getBuyerId()).map(User::getFullName).orElse("Deleted account");
        String businessName = businessRepository.findById(order.getBusinessId()).map(Business::getBusinessName).orElse("Unknown business");
        return OrderDTO.from(order, buyerName, businessName);
    }
}
