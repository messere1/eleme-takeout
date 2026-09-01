package cn.edu.tju.takeout.order;

import java.math.BigDecimal;

public record OrderItemView(
        Long productId, String productName, BigDecimal unitPrice,
        Integer quantity, BigDecimal subtotal) {
    public static OrderItemView from(OrderItem item) {
        return new OrderItemView(
                item.getProductId(), item.getProductName(), item.getUnitPrice(),
                item.getQuantity(), item.getSubtotal());
    }
}
