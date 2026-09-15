package cn.edu.tju.takeout.recommend;

import cn.edu.tju.takeout.shop.Shop;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
class RecommendationScoring {
    static final double SHOP_WEIGHT = 3.0;
    static final double CATEGORY_WEIGHT = 1.5;
    static final double KEYWORD_WEIGHT = 1.0;
    static final double POPULARITY_WEIGHT = 0.3;

    /** 半衰期：14 天前的行为权重降到一半 */
    static final double DECAY_HALF_LIFE_DAYS = 14.0;

    /** 负反馈扣分权重。取值高于单次复购(3.0)的一半：取消两次就足以压过一次复购 */
    static final double NEGATIVE_WEIGHT = 2.0;

    /** 时段匹配权重。只做同分附近的微调，压不过一次关键词命中 */
    static final double TIME_FIT_WEIGHT = 0.8;

    /** 打分并降序排序；同分按店铺 id 升序，保证结果稳定可复现 */
    List<ScoredShop> score(List<Shop> candidates, ShopFacts facts, RecommendationSignals signals) {
        Preferences preferences = signals.preferences();
        Map<Long, Double> popularity = signals.popularity();

        List<ScoredShop> scored = new ArrayList<>(candidates.size());
        for (Shop shop : candidates) {
            double score = SHOP_WEIGHT * preferences.shops().getOrDefault(shop.getId(), 0.0);

            Set<Long> categoryIds = facts.categoriesByShop().getOrDefault(shop.getId(), Set.of());
            for (Long categoryId : categoryIds) {
                if (preferences.categories().containsKey(categoryId)) {
                    score += CATEGORY_WEIGHT * preferences.categories().get(categoryId);
                }
            }

            score += keywordScore(matchTexts(shop, facts), preferences.keywords());
            score += POPULARITY_WEIGHT * popularity.getOrDefault(shop.getId(), 0.0);
            score += timeFitScore(categoryIds, facts, signals.mealPeriod());

            // 负反馈扣分。退款通过的店已被召回阶段排除，这里只会碰到「取消过」的店。
            score -= NEGATIVE_WEIGHT * signals.negative().penalties().getOrDefault(shop.getId(), 0.0);

            scored.add(new ScoredShop(shop, score));
        }

        scored.sort(Comparator.comparingDouble(ScoredShop::score).reversed()
                .thenComparing(item -> item.shop().getId()));
        return scored;
    }

    /** 关键词的匹配范围：店铺名、经营范围、经营品类名、在售商品名，与搜索接口一致 */
    private static List<String> matchTexts(Shop shop, ShopFacts facts) {
        List<String> texts = new ArrayList<>();
        if (shop.getShopName() != null) texts.add(shop.getShopName());

        String scope = facts.businessScopeByShop().get(shop.getId());
        if (scope != null) texts.add(scope);

        for (Long categoryId : facts.categoriesByShop().getOrDefault(shop.getId(), Set.of())) {
            String categoryName = facts.categoryNames().get(categoryId);
            if (categoryName != null) texts.add(categoryName);
        }

        texts.addAll(facts.productNamesByShop().getOrDefault(shop.getId(), List.of()));
        return texts;
    }

    private static double keywordScore(List<String> texts, Map<String, Double> keywords) {
        if (texts.isEmpty() || keywords.isEmpty()) return 0.0;

        List<String> lowered = new ArrayList<>(texts.size());
        for (String text : texts) {
            lowered.add(text.toLowerCase(Locale.ROOT));
        }

        double score = 0.0;
        for (Map.Entry<String, Double> entry : keywords.entrySet()) {
            String needle = entry.getKey().toLowerCase(Locale.ROOT);
            for (String text : lowered) {
                if (text.contains(needle)) {
                    score += KEYWORD_WEIGHT * entry.getValue();
                    break;
                }
            }
        }
        return score;
    }

    /** 经营品类对得上当前餐段就加分。一家店只加一次，命中几个相关品类不叠加。 */
    private static double timeFitScore(
            Set<Long> categoryIds, ShopFacts facts, MealPeriod mealPeriod) {
        if (mealPeriod == null) return 0.0;
        for (Long categoryId : categoryIds) {
            if (MealPeriod.fits(facts.categoryNames().get(categoryId), mealPeriod)) {
                return TIME_FIT_WEIGHT;
            }
        }
        return 0.0;
    }

    /** 次数按时间衰减：越久远的行为权重越低 */
    static double decay(int times, LocalDateTime lastAt, LocalDateTime now) {
        if (lastAt == null) return times;
        double days = Duration.between(lastAt, now).toSeconds() / 86400.0;
        if (days < 0) days = 0;
        return times * Math.pow(0.5, days / DECAY_HALF_LIFE_DAYS);
    }

    /** 热度归一到 0~1，避免订单量绝对值压过个人偏好 */
    static Map<Long, Double> normalizePopularity(List<RecommendationMapper.ShopCount> rows) {
        int max = 0;
        for (RecommendationMapper.ShopCount row : rows) {
            if (row.times() > max) max = row.times();
        }
        if (max == 0) return Map.of();

        Map<Long, Double> normalized = new java.util.HashMap<>();
        for (RecommendationMapper.ShopCount row : rows) {
            normalized.put(row.shopId(), row.times() / (double) max);
        }
        return normalized;
    }
}
