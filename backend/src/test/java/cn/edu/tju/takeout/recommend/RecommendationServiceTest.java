package cn.edu.tju.takeout.recommend;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import cn.edu.tju.takeout.shop.Shop;
import cn.edu.tju.takeout.shop.ShopPage;
import cn.edu.tju.takeout.shop.ShopView;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RecommendationServiceTest {
    private static final int KEYWORD_LIMIT = 20;

    @Mock private RecommendationMapper recommendationMapper;
    @Mock private SearchHistoryMapper searchHistoryMapper;
    private RecommendationService service;

    // 默认桩模拟真实 mapper：给什么 id 就返回什么店铺，不写死具体 id。
    // 设成 lenient，各用例再按需要覆盖。
    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        service = new RecommendationService(
                recommendationMapper,
                searchHistoryMapper,
                new ShopRecallService(recommendationMapper),
                new RecommendationScoring(),
                new RecommendationRerank());

        lenient().when(recommendationMapper.findOpenShopsByIds(anyCollection())).thenAnswer(invocation ->
                ((Collection<Long>) invocation.getArgument(0)).stream()
                        .map(RecommendationServiceTest::shop)
                        .toList());
        lenient().when(recommendationMapper.findShopCategories(anyCollection())).thenReturn(List.of());
        lenient().when(recommendationMapper.findShopProductNames(anyCollection())).thenReturn(List.of());
        lenient().when(recommendationMapper.findShopAffinity(anyLong())).thenReturn(List.of());
        lenient().when(recommendationMapper.findCategoryAffinity(anyLong())).thenReturn(List.of());
        lenient().when(recommendationMapper.findNegativeAffinity(anyLong())).thenReturn(List.of());
        lenient().when(recommendationMapper.findRefundedShopIds(anyLong())).thenReturn(List.of());
        lenient().when(recommendationMapper.findShopBusinessScopes(anyCollection()))
                .thenReturn(List.of());
        lenient().when(recommendationMapper.findOpenShopIdsByBusinessCategories(anyCollection()))
                .thenReturn(List.of());
        lenient().when(recommendationMapper.findOpenShopIdsByKeyword(anyCollection()))
                .thenReturn(List.of());
        lenient().when(recommendationMapper.findOpenShopIds(anyInt())).thenAnswer(invocation -> {
            int wanted = invocation.getArgument(0);
            List<Long> ids = new ArrayList<>();
            for (long id = 1; id <= Math.min(wanted, 60); id++) ids.add(id);
            return ids;
        });
        lenient().when(recommendationMapper.findRecentPopularity(any(), anyInt())).thenReturn(List.of());
        lenient().when(searchHistoryMapper.findTopKeywords(anyLong(), anyInt())).thenReturn(List.of());
    }

    @Test
    void pageOrderIsUntouchedWhenUserHasNoHistory() {
        ShopPage page = page(view(15L), view(13L));

        assertThat(service.reorderByPreference(7L, page)).isSameAs(page);
    }

    @Test
    void guestPageIsUntouched() {
        ShopPage page = page(view(15L), view(13L));

        assertThat(service.reorderByPreference(null, page)).isSameAs(page);
    }

    @Test
    void repurchasedShopMovesToFrontAndOthersKeepRelativeOrder() {
        when(recommendationMapper.findShopAffinity(7L))
                .thenReturn(List.of(new RecommendationMapper.Affinity(1L, 3, LocalDateTime.now())));

        ShopPage result = service.reorderByPreference(7L, page(view(15L), view(13L), view(1L)));

        assertThat(result.items()).extracting(ShopView::id).containsExactly(1L, 15L, 13L);
    }

    @Test
    void searchKeywordMovesShopsSellingMatchingProductsToFront() {
        when(searchHistoryMapper.findTopKeywords(7L, KEYWORD_LIMIT)).thenReturn(List.of(
                new SearchHistoryMapper.KeywordStat("煎饼", 2, LocalDateTime.now())));
        // 召回通道：关键词能捞出的店铺
        when(recommendationMapper.findOpenShopIdsByKeyword(anyCollection()))
                .thenReturn(List.of(1L));
        when(recommendationMapper.findShopProductNames(anyCollection())).thenReturn(List.of(
                new RecommendationMapper.ShopName(1L, "煎饼果子")));

        ShopPage result = service.reorderByPreference(7L, page(view(15L), view(1L)));

        assertThat(result.items()).extracting(ShopView::id).containsExactly(1L, 15L);
    }

    @Test
    void preferredCategoryMovesShopsInThatCategoryToFront() {
        when(recommendationMapper.findCategoryAffinity(7L))
                .thenReturn(List.of(new RecommendationMapper.Affinity(3L, 1, LocalDateTime.now())));
        // 召回通道：偏好品类下的店铺
        when(recommendationMapper.findOpenShopIdsByBusinessCategories(anyCollection()))
                .thenReturn(List.of(6L));
        when(recommendationMapper.findShopCategories(anyCollection())).thenReturn(List.of(
                new RecommendationMapper.ShopTarget(6L, 3L),
                new RecommendationMapper.ShopTarget(15L, 2L)));

        ShopPage result = service.reorderByPreference(7L, page(view(15L), view(6L)));

        assertThat(result.items()).extracting(ShopView::id).containsExactly(6L, 15L);
    }

    @Test
    void coldStartFallsBackToRecentPopularity() {
        when(recommendationMapper.findRecentPopularity(any(), anyInt())).thenReturn(List.of(
                new RecommendationMapper.ShopCount(1L, 20),
                new RecommendationMapper.ShopCount(15L, 5)));

        List<ShopView> recommended = service.recommendShops(null, 2);

        assertThat(recommended).extracting(ShopView::id).containsExactly(1L, 15L);
    }

    @Test
    void recentRepurchaseOutranksStaleRepurchaseOfTheSameShop() {
        LocalDateTime now = LocalDateTime.now();
        when(recommendationMapper.findShopAffinity(7L)).thenReturn(List.of(
                new RecommendationMapper.Affinity(1L, 1, now.minusDays(90)),
                new RecommendationMapper.Affinity(2L, 1, now)));

        ShopPage result = service.reorderByPreference(7L, page(view(1L), view(2L)));

        assertThat(result.items()).extracting(ShopView::id).containsExactly(2L, 1L);
    }

    /** 兜底池大小必须与请求的 limit 无关，否则「前 N 条」不是稳定前缀 */
    @Test
    void candidatePoolDoesNotDependOnRequestedLimit() {
        service.recommendShops(null, 2);
        service.recommendShops(null, 6);

        verify(recommendationMapper, times(2)).findOpenShopIds(50);
    }

    @Test
    void limitDefaultsToSixAndIsClampedToOneThroughFifty() {
        assertThat(service.recommendShops(null, null)).hasSize(6);
        assertThat(service.recommendShops(null, 3)).hasSize(3);
        assertThat(service.recommendShops(null, 0)).hasSize(1);
        assertThat(service.recommendShops(null, -5)).hasSize(1);
        assertThat(service.recommendShops(null, 500)).hasSize(50);
    }

    @Test
    void reorderPreservesPageMetadata() {
        when(recommendationMapper.findShopAffinity(7L))
                .thenReturn(List.of(new RecommendationMapper.Affinity(1L, 1, LocalDateTime.now())));

        ShopPage result = service.reorderByPreference(
                7L, new ShopPage(List.of(view(2L), view(1L)), 3, 5, 42, 9));

        assertThat(result.page()).isEqualTo(3);
        assertThat(result.size()).isEqualTo(5);
        assertThat(result.total()).isEqualTo(42);
        assertThat(result.totalPages()).isEqualTo(9);
    }

    /** 一个关键词对一家店最多加一次分：库存多的店不该靠菜名数量压过真实复购 */
    @Test
    void keywordHitCountsOncePerShopNoMatterHowManyProductsMatch() {
        List<RecommendationMapper.ShopName> products = new ArrayList<>();
        for (int i = 0; i < 40; i++) {
            products.add(new RecommendationMapper.ShopName(1L, "炸鸡" + i));
        }
        when(recommendationMapper.findShopAffinity(7L))
                .thenReturn(List.of(new RecommendationMapper.Affinity(3L, 1, LocalDateTime.now())));
        when(searchHistoryMapper.findTopKeywords(7L, KEYWORD_LIMIT)).thenReturn(List.of(
                new SearchHistoryMapper.KeywordStat("炸鸡", 1, LocalDateTime.now())));
        when(recommendationMapper.findOpenShopIdsByKeyword(anyCollection()))
                .thenReturn(List.of(1L));
        when(recommendationMapper.findShopProductNames(anyCollection())).thenReturn(products);

        ShopPage result = service.reorderByPreference(7L, page(view(1L), view(2L), view(3L)));

        // 3 号店一次复购(3.0) 胜 1 号店 40 个命中商品(1.0)，2 号店无信号垫底
        assertThat(result.items()).extracting(ShopView::id).containsExactly(3L, 1L, 2L);
    }

    /** 搜索用 ILIKE 不区分大小写，关键词打分必须跟着不区分 */
    @Test
    void keywordMatchIgnoresCase() {
        when(searchHistoryMapper.findTopKeywords(7L, KEYWORD_LIMIT)).thenReturn(List.of(
                new SearchHistoryMapper.KeywordStat("kfc", 1, LocalDateTime.now())));
        when(recommendationMapper.findOpenShopIdsByKeyword(anyCollection()))
                .thenReturn(List.of(1L));
        when(recommendationMapper.findShopProductNames(anyCollection())).thenReturn(List.of(
                new RecommendationMapper.ShopName(1L, "KFC 全家桶")));

        ShopPage result = service.reorderByPreference(7L, page(view(2L), view(1L)));

        assertThat(result.items()).extracting(ShopView::id).containsExactly(1L, 2L);
    }

    /** 取消一次扣 2.0，同样的复购次数下会被没取消过的店反超 */
    @Test
    void cancelledShopSinksBelowAnEquallyFrequentOne() {
        LocalDateTime now = LocalDateTime.now();
        when(recommendationMapper.findShopAffinity(7L)).thenReturn(List.of(
                new RecommendationMapper.Affinity(1L, 1, now),
                new RecommendationMapper.Affinity(2L, 1, now)));
        when(recommendationMapper.findNegativeAffinity(7L)).thenReturn(List.of(
                new RecommendationMapper.Affinity(1L, 1, now)));

        ShopPage result = service.reorderByPreference(7L, page(view(1L), view(2L)));

        assertThat(result.items()).extracting(ShopView::id).containsExactly(2L, 1L);
    }

    /** 退款通过的店即使复购再多也不参与重排 */
    @Test
    void refundedShopIsExcludedEvenWhenRepurchasedOften() {
        when(recommendationMapper.findShopAffinity(7L))
                .thenReturn(List.of(new RecommendationMapper.Affinity(1L, 5, LocalDateTime.now())));
        when(recommendationMapper.findRefundedShopIds(7L)).thenReturn(List.of(1L));

        ShopPage result = service.reorderByPreference(7L, page(view(3L), view(1L), view(2L)));

        // 1 号店被排除，谁都没拿到名次，整页顺序不变；若没排除则 1 号店会跳到最前
        assertThat(result.items()).extracting(ShopView::id).containsExactly(3L, 1L, 2L);
    }

    /** 复购信号再强，也不该给一家按营业时间已经打烊的店 */
    @Test
    void shopOutsideItsBusinessHoursIsNotBoosted() {
        LocalTime now = LocalTime.now();
        when(recommendationMapper.findShopAffinity(7L))
                .thenReturn(List.of(new RecommendationMapper.Affinity(1L, 5, LocalDateTime.now())));
        // 营业窗口落在两小时前，此刻必定已打烊
        when(recommendationMapper.findOpenShopsByIds(anyCollection())).thenReturn(List.of(
                shopDuring(1L, now.minusHours(3), now.minusHours(2))));

        ShopPage result = service.reorderByPreference(7L, page(view(1L), view(2L)));

        assertThat(result.items()).extracting(ShopView::id).containsExactly(1L, 2L);
    }

    @Test
    void shopInsideItsBusinessHoursIsStillBoosted() {
        LocalTime now = LocalTime.now();
        when(recommendationMapper.findShopAffinity(7L))
                .thenReturn(List.of(new RecommendationMapper.Affinity(1L, 1, LocalDateTime.now())));
        when(recommendationMapper.findOpenShopsByIds(anyCollection())).thenReturn(List.of(
                shopDuring(1L, now.minusHours(1), now.plusHours(1))));

        ShopPage result = service.reorderByPreference(7L, page(view(2L), view(1L)));

        assertThat(result.items()).extracting(ShopView::id).containsExactly(1L, 2L);
    }

    /** 关键词的匹配范围和搜索接口一致：按店名搜到的店，也得拿到加分 */
    @Test
    void searchingAShopNameBoostsThatShop() {
        when(searchHistoryMapper.findTopKeywords(7L, KEYWORD_LIMIT)).thenReturn(List.of(
                new SearchHistoryMapper.KeywordStat("北洋", 1, LocalDateTime.now())));
        when(recommendationMapper.findOpenShopIdsByKeyword(anyCollection())).thenReturn(List.of(1L));
        when(recommendationMapper.findOpenShopsByIds(anyCollection()))
                .thenReturn(List.of(namedShop(1L, "北洋餐厅")));

        ShopPage result = service.reorderByPreference(7L, page(view(2L), view(1L)));

        assertThat(result.items()).extracting(ShopView::id).containsExactly(1L, 2L);
    }

    /** 店名和商品名都不含关键词，只有经营范围命中，同样要加分 */
    @Test
    void searchingABusinessScopeBoostsShopsUnderIt() {
        when(searchHistoryMapper.findTopKeywords(7L, KEYWORD_LIMIT)).thenReturn(List.of(
                new SearchHistoryMapper.KeywordStat("火锅", 1, LocalDateTime.now())));
        when(recommendationMapper.findOpenShopIdsByKeyword(anyCollection())).thenReturn(List.of(1L));
        when(recommendationMapper.findShopBusinessScopes(anyCollection())).thenReturn(List.of(
                new RecommendationMapper.ShopScope(1L, "重庆火锅")));
        when(recommendationMapper.findOpenShopsByIds(anyCollection()))
                .thenReturn(List.of(namedShop(1L, "甲乙丙")));

        ShopPage result = service.reorderByPreference(7L, page(view(2L), view(1L)));

        assertThat(result.items()).extracting(ShopView::id).containsExactly(1L, 2L);
    }

    /** 搜索接口能按经营品类名搜到店，推荐的关键词打分也得认这个词 */
    @Test
    void searchingABusinessCategoryNameBoostsShopsInIt() {
        when(searchHistoryMapper.findTopKeywords(7L, KEYWORD_LIMIT)).thenReturn(List.of(
                new SearchHistoryMapper.KeywordStat("汉堡披萨", 1, LocalDateTime.now())));
        when(recommendationMapper.findOpenShopIdsByKeyword(anyCollection())).thenReturn(List.of(1L));
        when(recommendationMapper.findShopCategories(anyCollection())).thenReturn(List.of(
                new RecommendationMapper.ShopTarget(1L, 10L)));
        when(recommendationMapper.findAllBusinessCategories()).thenReturn(List.of(
                new RecommendationMapper.BusinessCategory(10L, "汉堡披萨")));
        // 店名和商品名都不含这个词，只有品类名命中
        when(recommendationMapper.findOpenShopsByIds(anyCollection()))
                .thenReturn(List.of(namedShop(1L, "甲乙丙")));

        ShopPage result = service.reorderByPreference(7L, page(view(2L), view(1L)));

        assertThat(result.items()).extracting(ShopView::id).containsExactly(1L, 2L);
    }

    private static ShopPage page(ShopView... items) {
        return new ShopPage(List.of(items), 1, 20, items.length, 1);
    }

    private static ShopView view(Long id) {
        return new ShopView(id, 12L, "店" + id, "公告", "OPEN");
    }

    private static Shop shop(Long id) {
        Shop shop = Shop.initiallyClosed(12L, "店" + id);
        shop.setId(id);
        return shop;
    }

    private static Shop namedShop(Long id, String name) {
        Shop shop = Shop.initiallyClosed(12L, name);
        shop.setId(id);
        return shop;
    }

    private static Shop shopDuring(Long id, LocalTime opening, LocalTime closing) {
        Shop shop = shop(id);
        shop.updateBusinessHours(opening, closing);
        return shop;
    }
}
