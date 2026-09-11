package cn.edu.tju.takeout.cart;

import java.math.BigDecimal;

public class CartLine {
    private Long id;
    private Long productId;
    private Long shopId;
    private String productName;
    private BigDecimal price;
    private Integer quantity;
    private Integer stock;
    private String status;

    public static CartLine of(
            Long id, Long productId, String productName, BigDecimal price,
            Integer quantity, Integer stock, String status) {
        CartLine line = new CartLine();
        line.id = id;
        line.productId = productId;
        line.productName = productName;
        line.price = price;
        line.quantity = quantity;
        line.stock = stock;
        line.status = status;
        return line;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getProductId() { return productId; }
    public Long getShopId() { return shopId; }
    public void setShopId(Long shopId) { this.shopId = shopId; }
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
