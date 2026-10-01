package za.co.unilinkhub.ordering.application;

import za.co.unilinkhub.ordering.domain.Order;
import za.co.unilinkhub.ordering.domain.OrderItem;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record OrderDTO(
        UUID id,
        UUID buyerId,
        String buyerName,
        UUID businessId,
        String businessName,
        String status,
        String fulfilmentMethod,
        String note,
        String promoCode,
        BigDecimal subtotal,
        BigDecimal discountAmount,
        BigDecimal total,
        String cancelReason,
        List<OrderItemDTO> items,
        LocalDateTime createdAt,
        /** Only ever filled in for the buyer - the seller must ask for it at handover. */
        String pickupCode,
        boolean requiresPickupCode
) {
    public record OrderItemDTO(UUID listingId, String listingName, BigDecimal unitPrice, int quantity) {
        public static OrderItemDTO from(OrderItem item) {
            return new OrderItemDTO(item.getListingId(), item.getListingName(), item.getUnitPrice(), item.getQuantity());
        }
    }

    public static OrderDTO from(Order order, String buyerName, String businessName, boolean forBuyer) {
        return new OrderDTO(
                order.getId(), order.getBuyerId(), buyerName, order.getBusinessId(), businessName,
                order.getStatus().name(), order.getFulfilmentMethod(), order.getNote(), order.getPromoCode(),
                order.getSubtotal(), order.getDiscountAmount(), order.getTotal(), order.getCancelReason(),
                order.getItems().stream().map(OrderItemDTO::from).toList(), order.getCreatedAt(),
                forBuyer ? order.getPickupCode() : null, order.getPickupCode() != null
        );
    }
}
