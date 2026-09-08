package cn.edu.tju.takeout.product;

import java.util.List;

public record ProductPage(
        List<ProductView> items,
        int page,
        int size,
        long total,
        int totalPages
) {
}
