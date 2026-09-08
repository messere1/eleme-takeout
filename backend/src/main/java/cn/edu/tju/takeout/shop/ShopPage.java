package cn.edu.tju.takeout.shop;

import java.util.List;

public record ShopPage(
        List<ShopView> items,
        int page,
        int size,
        long total,
        int totalPages
) {
}