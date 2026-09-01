package cn.edu.tju.takeout.order;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record OrderSummaryView(
        Long id, String orderNo, Long shopId, BigDecimal totalAmount,
        String status, LocalDateTime createdAt) {
    public static OrderSummaryView from(Order order) {
        return new OrderSummaryView(
                order.getId(), order.getOrderNo(), order.getShopId(), order.getTotalAmount(),
                order.getStatus(), order.getCreatedAt());
    }
}
