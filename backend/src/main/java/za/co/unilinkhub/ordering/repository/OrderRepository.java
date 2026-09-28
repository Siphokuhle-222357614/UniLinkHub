package za.co.unilinkhub.ordering.repository;

import za.co.unilinkhub.ordering.domain.Order;
import za.co.unilinkhub.ordering.domain.OrderStatus;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OrderRepository {

    Order save(Order order);

    Optional<Order> findById(UUID id);

    List<Order> findByBuyerId(UUID buyerId);

    List<Order> findByBusinessId(UUID businessId);

    List<Order> findAll();

    long countByStatus(OrderStatus status);
}
