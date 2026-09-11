package cn.edu.tju.takeout.cart;

import java.math.BigDecimal;

public record CartLineView(
        Long id, Long shopId, Long productId, String productName, BigDecimal price, Integer quantity,
        BigDecimal subtotal, boolean available, String unavailableReason) {

    public static CartLineView from(CartLine line) {
        String reason = null;
        if (!"ON_SALE".equals(line.getStatus())) {
            reason = "OFF_SALE";
        } else if (line.getQuantity() > line.getStock()) {
            reason = "INSUFFICIENT_STOCK";
        }
        BigDecimal subtotal = line.getPrice().multiply(BigDecimal.valueOf(line.getQuantity()));
        return new CartLineView(
                line.getId(), line.getShopId(), line.getProductId(), line.getProductName(), line.getPrice(),
                line.getQuantity(), subtotal, reason == null, reason);
    }
}
