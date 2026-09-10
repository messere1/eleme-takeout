package cn.edu.tju.takeout.product;

import java.math.BigDecimal;

public record ProductView(
        Long id, Long shopId, Long categoryId, String name, String description,
        BigDecimal price, Integer stock, String status, String imageUrl) {
    public ProductView(Long id, Long shopId, Long categoryId, String name, String description,
                       BigDecimal price, Integer stock, String status) {
        this(id, shopId, categoryId, name, description, price, stock, status, null);
    }
    public static ProductView from(Product product) {
        return new ProductView(
                product.getId(), product.getShopId(), product.getCategoryId(), product.getName(),
                product.getDescription(), product.getPrice(), product.getStock(), product.getStatus(), product.getImageUrl());
    }
}
