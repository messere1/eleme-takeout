package cn.edu.tju.takeout.recommend;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 打分需要的店铺侧事实，按店铺 id 分组后传入，只查召回出来的候选店铺。
 * categoryNames 是经营品类 id 到品类名的全量映射（表很小），用于判断餐段匹配。
 */
record ShopFacts(
        Map<Long, Set<Long>> categoriesByShop,
        Map<Long, List<String>> productNamesByShop,
        Map<Long, String> businessScopeByShop,
        Map<Long, String> categoryNames) {
    static final ShopFacts EMPTY = new ShopFacts(Map.of(), Map.of(), Map.of(), Map.of());
}
