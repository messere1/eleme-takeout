package cn.edu.tju.takeout.cart;

import java.math.BigDecimal;

public class CartCheckoutLine {
    private Long cartItemId;
    private Long shopId;
    private Long productId;
    private String productName;
    private BigDecimal price;
    private Integer quantity;
    private Integer stock;
    private String status;

    public static CartCheckoutLine of(
            Long cartItemId, Long shopId, Long productId, String productName,
            BigDecimal price, Integer quantity, Integer stock, String status) {
        CartCheckoutLine line = new CartCheckoutLine();
        line.cartItemId = cartItemId;
        line.shopId = shopId;
        line.productId = productId;
        line.productName = productName;
        line.price = price;
        line.quantity = quantity;
        line.stock = stock;
        line.status = status;
        return line;
    }

    public Long getCartItemId() { return cartItemId; }
    public void setCartItemId(Long cartItemId) { this.cartItemId = cartItemId; }
    public Long getShopId() { return shopId; }
    public void setShopId(Long shopId) { this.shopId = shopId; }
    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }
    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
    public Integer getStock() { return stock; }
    public void setStock(Integer stock) { this.stock = stock; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
