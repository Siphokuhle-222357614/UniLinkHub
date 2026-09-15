package za.co.unilinkhub.ordering.application;

import java.math.BigDecimal;
import java.util.List;

public record OrderStatsDTO(
        long totalOrders,
        BigDecimal grossValue,
        long placed,
        long confirmed,
        long ready,
        long completed,
        long cancelled,
        List<OrderDTO> recentOrders
) {
}
