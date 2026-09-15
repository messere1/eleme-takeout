package cn.edu.tju.takeout.order;

import cn.edu.tju.takeout.cart.CartCheckoutLine;
import java.math.BigDecimal;

public class OrderItem {
    private Long id;
    private Long orderId;
    private Long productId;
    private String productName;
    private BigDecimal unitPrice;
    private Integer quantity;
    private BigDecimal subtotal;
    private String imageUrl;

    public static OrderItem from(Long orderId, CartCheckoutLine line) {
        OrderItem item = new OrderItem();
        item.orderId = orderId;
        item.productId = line.getProductId();
        item.productName = line.getProductName();
        item.unitPrice = line.getPrice();
        item.quantity = line.getQuantity();
        item.subtotal = line.getPrice().multiply(BigDecimal.valueOf(line.getQuantity()))
                .setScale(2, java.math.RoundingMode.HALF_UP);
        return item;
    }

    public static OrderItem restore(
            Long id, Long orderId, Long productId, String productName,
            BigDecimal unitPrice, Integer quantity, BigDecimal subtotal) {
        OrderItem item = new OrderItem();
        item.id = id;
        item.orderId = orderId;
        item.productId = productId;
        item.productName = productName;
        item.unitPrice = unitPrice;
        item.quantity = quantity;
        item.subtotal = subtotal;
        return item;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getOrderId() { return orderId; }
    public Long getProductId() { return productId; }
    public String getProductName() { return productName; }
    public BigDecimal getUnitPrice() { return unitPrice; }
    public Integer getQuantity() { return quantity; }
    public BigDecimal getSubtotal() { return subtotal; }
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
}
