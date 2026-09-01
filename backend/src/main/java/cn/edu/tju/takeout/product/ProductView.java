package cn.edu.tju.takeout.product;

import java.math.BigDecimal;

public record ProductView(
        Long id, Long shopId, Long categoryId, String name, String description,
        BigDecimal price, Integer stock, String status) {
    public static ProductView from(Product product) {
        return new ProductView(
                product.getId(), product.getShopId(), product.getCategoryId(), product.getName(),
                product.getDescription(), product.getPrice(), product.getStock(), product.getStatus());
    }
}
