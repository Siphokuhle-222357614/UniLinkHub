package za.co.unilinkhub.ordering.domain.factory;

import za.co.unilinkhub.ordering.domain.Order;
import za.co.unilinkhub.ordering.domain.OrderItem;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Builds an Order from a set of cart lines for a single business, working out the subtotal
 * and final total so that math lives in one place rather than being repeated by every caller.
 */
public class OrderFactory {

    public static Order create(UUID buyerId, UUID businessId, List<OrderItem> items, String fulfilmentMethod,
                                String note, String promoCode, BigDecimal discountAmount) {
        BigDecimal subtotal = items.stream()
                .map(i -> i.getUnitPrice().multiply(BigDecimal.valueOf(i.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal discount = discountAmount == null ? BigDecimal.ZERO : discountAmount;
        BigDecimal total = subtotal.subtract(discount).max(BigDecimal.ZERO);

        return Order.create(buyerId, businessId, items, fulfilmentMethod, note, promoCode, subtotal, discount, total);
    }
}
