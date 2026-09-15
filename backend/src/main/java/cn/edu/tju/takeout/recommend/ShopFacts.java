package cn.edu.tju.takeout.recommend;

import java.util.List;
import java.util.Map;
import java.util.Set;

record ShopFacts(
        Map<Long, Set<Long>> categoriesByShop,
        Map<Long, List<String>> productNamesByShop,
        Map<Long, String> businessScopeByShop,
        Map<Long, String> categoryNames) {
    static final ShopFacts EMPTY = new ShopFacts(Map.of(), Map.of(), Map.of(), Map.of());
}
