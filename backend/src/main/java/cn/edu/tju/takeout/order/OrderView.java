package cn.edu.tju.takeout.order;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record OrderView(
        Long id, String orderNo, Long shopId, BigDecimal totalAmount, String status,
        LocalDateTime createdAt, List<OrderItemView> items) {
    public static OrderView from(Order order, List<OrderItem> items) {
        return new OrderView(
                order.getId(), order.getOrderNo(), order.getShopId(), order.getTotalAmount(),
                order.getStatus(), order.getCreatedAt(), items.stream().map(OrderItemView::from).toList());
    }
}
