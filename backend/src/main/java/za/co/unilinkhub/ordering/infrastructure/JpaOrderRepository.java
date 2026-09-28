package za.co.unilinkhub.ordering.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;
import za.co.unilinkhub.ordering.domain.Order;
import za.co.unilinkhub.ordering.repository.OrderRepository;

import java.util.List;
import java.util.UUID;

public interface JpaOrderRepository extends JpaRepository<Order, UUID>, OrderRepository {
    @Override
    List<Order> findByBuyerId(UUID buyerId);

    @Override
    List<Order> findByBusinessId(UUID businessId);
}
