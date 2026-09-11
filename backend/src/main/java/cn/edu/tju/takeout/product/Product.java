package cn.edu.tju.takeout.product;

import java.math.BigDecimal;

public class Product {
    private Long id;
    private Long shopId;
    private Long categoryId;
    private String name;
    private String description;
    private BigDecimal price;
    private Integer stock;
    private String status;
    private boolean deleted;
    private String imageUrl;

    public static Product of(
            Long id, Long shopId, Long categoryId, String name, String description,
            BigDecimal price, Integer stock, String status) {
        Product product = new Product();
        product.id = id;
        product.shopId = shopId;
        product.categoryId = categoryId;
        product.name = name;
        product.description = description;
        product.price = price;
        product.stock = stock;
        product.status = status;
        return product;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getShopId() { return shopId; }
    public Long getCategoryId() { return categoryId; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public BigDecimal getPrice() { return price; }
    public Integer getStock() { return stock; }
    public String getStatus() { return status; }
    public boolean isDeleted() { return deleted; }
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public void update(ProductRequest request) {
        categoryId = request.categoryId();
        name = request.name();
        description = request.description();
        price = request.price();
        stock = request.stock();
    }

    public void changeStatus(String status) { this.status = status; }
    public void updatePrice(BigDecimal price) { this.price = price; }
    public void updateStock(Integer stock) { this.stock = stock; }
}
