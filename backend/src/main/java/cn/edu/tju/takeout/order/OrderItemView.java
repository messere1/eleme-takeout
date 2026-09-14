package cn.edu.tju.takeout.order;

import java.math.BigDecimal;

public record OrderItemView(
        Long productId, String productName, BigDecimal unitPrice,
        Integer quantity, BigDecimal subtotal,
        /* 商品图，来自查询时 JOIN products；商品没有图时为 null，前端退化成占位图。 */
        String imageUrl) {
    public static OrderItemView from(OrderItem item) {
        return new OrderItemView(
                item.getProductId(), item.getProductName(), item.getUnitPrice(),
                item.getQuantity(), item.getSubtotal(), item.getImageUrl());
    }
}
