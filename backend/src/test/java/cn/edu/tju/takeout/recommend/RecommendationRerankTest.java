package cn.edu.tju.takeout.recommend;

import static org.assertj.core.api.Assertions.assertThat;

import cn.edu.tju.takeout.shop.Shop;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class RecommendationRerankTest {
    private final RecommendationRerank rerank = new RecommendationRerank();

    /** 前三名同一个品类，第四名换个品类：把第四名提到第三位，打断品类连坐 */
    @Test
    void breaksUpCategoryRuns() {
        ShopFacts facts = categories(Map.of(
                1L, Set.of(10L), 2L, Set.of(10L), 3L, Set.of(10L), 4L, Set.of(20L)));

        List<ScoredShop> result = rerank.apply(
                List.of(scored(1, 9), scored(2, 8), scored(3, 7), scored(4, 6)),
                Preferences.EMPTY,
                facts);

        assertThat(ids(result)).containsExactly(1L, 2L, 4L, 3L);
        assertThat(result).hasSize(4);
    }

    /** 连推 2 家复购店之后必须插一家探索店 */
    @Test
    void insertsExploreShopAfterTwoRepeats() {
        Preferences preferences = new Preferences(
                Map.of(1L, 1.0, 2L, 1.0, 3L, 1.0), Map.of(), Map.of());

        List<ScoredShop> result = rerank.apply(
                List.of(scored(1, 9), scored(2, 8), scored(3, 7), scored(4, 6)),
                preferences,
                ShopFacts.EMPTY);

        assertThat(ids(result)).containsExactly(1L, 2L, 4L, 3L);
    }

    /** 约束互相冲突且没有别的选择时退回分数顺序，不能把店丢掉或重复 */
    @Test
    void relaxesToScoreOrderWhenNoAlternativeExists() {
        ShopFacts facts = categories(Map.of(
                1L, Set.of(10L), 2L, Set.of(10L), 3L, Set.of(10L)));

        List<ScoredShop> result = rerank.apply(
                List.of(scored(1, 9), scored(2, 8), scored(3, 7)), Preferences.EMPTY, facts);

        assertThat(ids(result)).containsExactly(1L, 2L, 3L);
    }

    /** 没有品类的店不参与品类连坐判断，否则会被误判成同品类 */
    @Test
    void shopsWithoutCategoriesAreNeverGrouped() {
        List<ScoredShop> result = rerank.apply(
                List.of(scored(1, 9), scored(2, 8), scored(3, 7), scored(4, 6)),
                Preferences.EMPTY,
                ShopFacts.EMPTY);

        assertThat(ids(result)).containsExactly(1L, 2L, 3L, 4L);
    }

    /** 只有一家店时不做任何处理，也不该报错 */
    @Test
    void singleShopIsReturnedAsIs() {
        List<ScoredShop> single = List.of(scored(1, 9));

        assertThat(rerank.apply(single, Preferences.EMPTY, ShopFacts.EMPTY)).isSameAs(single);
    }

    private static ShopFacts categories(Map<Long, Set<Long>> byShop) {
        return new ShopFacts(byShop, Map.of(), Map.of(), Map.of());
    }

    private static List<Long> ids(List<ScoredShop> scored) {
        return scored.stream().map(item -> item.shop().getId()).toList();
    }

    private static ScoredShop scored(long id, double score) {
        Shop shop = Shop.initiallyClosed(12L, "店" + id);
        shop.setId(id);
        return new ScoredShop(shop, score);
    }
}
