package cn.edu.tju.takeout.recommend;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootTest
class RecommendationMapperIntegrationTest {
    private static final long USER_ID = 930001L;
    private static final long MERCHANT_OPEN = 930002L;
    private static final long MERCHANT_CLOSED_SHOP = 930003L;
    private static final long MERCHANT_DISABLED = 930004L;
    private static final long SHOP_OPEN = 930010L;
    private static final long SHOP_CLOSED = 930011L;
    private static final long SHOP_DISABLED_MERCHANT = 930012L;
    private static final long CATEGORY_OPEN = 930020L;
    private static final long CATEGORY_CLOSED = 930021L;
    private static final long BIZ_CATEGORY_A = 930030L;
    private static final long BIZ_CATEGORY_B = 930031L;
    private static final long ORDER_CANCELLED = 930042L;
    private static final long ORDER_REFUND_REJECTED = 930045L;
    private static final long REFUND_APPROVED = 930050L;
    private static final long REFUND_REJECTED = 930051L;

    @Autowired private JdbcTemplate jdbc;
    @Autowired private RecommendationMapper recommendationMapper;

    @BeforeEach
    void setUp() {
        cleanFixtures();
        LocalDateTime now = LocalDateTime.now();
        jdbc.update("""
                INSERT INTO users(id, username, phone, password_hash, nickname, created_at)
                VALUES (?, ?, ?, ?, ?, ?)
                """, USER_ID, "recommend-user", "13700009300", "x".repeat(60),
                "推荐测试用户", Timestamp.valueOf(now));

        merchant(MERCHANT_OPEN, "推荐测试-营业", true);
        merchant(MERCHANT_CLOSED_SHOP, "推荐测试-关店", true);
        merchant(MERCHANT_DISABLED, "推荐测试-停用", false);
        shop(SHOP_OPEN, MERCHANT_OPEN, "营业中店铺", "OPEN");
        shop(SHOP_CLOSED, MERCHANT_CLOSED_SHOP, "已关店铺", "CLOSED");
        shop(SHOP_DISABLED_MERCHANT, MERCHANT_DISABLED, "商家停用店铺", "OPEN");

        jdbc.update("""
                INSERT INTO business_categories(id, name, default_category, enabled)
                VALUES (?, ?, FALSE, TRUE)
                """, BIZ_CATEGORY_A, "推荐测试品类A");
        jdbc.update("""
                INSERT INTO business_categories(id, name, default_category, enabled)
                VALUES (?, ?, FALSE, TRUE)
                """, BIZ_CATEGORY_B, "推荐测试品类B");
        jdbc.update("""
                INSERT INTO shop_business_categories(shop_id, business_category_id)
                VALUES (?, ?), (?, ?)
                """, SHOP_OPEN, BIZ_CATEGORY_A, SHOP_CLOSED, BIZ_CATEGORY_B);

        category(CATEGORY_OPEN, SHOP_OPEN, "热菜");
        category(CATEGORY_CLOSED, SHOP_CLOSED, "别家菜");
        product(SHOP_OPEN, CATEGORY_OPEN, "红烧肉", "ON_SALE", false);
        product(SHOP_OPEN, CATEGORY_OPEN, "红烧排骨", "ON_SALE", false);
        product(SHOP_OPEN, CATEGORY_OPEN, "下架菜", "OFF_SALE", false);
        product(SHOP_OPEN, CATEGORY_OPEN, "已删除菜", "ON_SALE", true);
        product(SHOP_OPEN, CATEGORY_OPEN, "Cola 可乐", "ON_SALE", false);
        product(SHOP_CLOSED, CATEGORY_CLOSED, "别家菜", "ON_SALE", false);

        order(930040L, "REC-PAID", SHOP_OPEN, "PAID", "CREATED", now);
        order(930041L, "REC-UNPAID", SHOP_OPEN, "UNPAID", "CREATED", now);
        order(ORDER_CANCELLED, "REC-CANCELLED", SHOP_OPEN, "PAID", "CANCELLED", now);
        order(930043L, "REC-OLD", SHOP_OPEN, "PAID", "CREATED", now.minusDays(60));
        order(930044L, "REC-CLOSED-SHOP", SHOP_CLOSED, "PAID", "CREATED", now);
        // 挂在一个不参与热度断言的店铺上，避免污染 SHOP_CLOSED 的订单数
        order(ORDER_REFUND_REJECTED, "REC-REFUND-REJECTED", SHOP_DISABLED_MERCHANT, "PAID", "CREATED", now);

        refund(REFUND_APPROVED, ORDER_CANCELLED, "APPROVED");
        refund(REFUND_REJECTED, ORDER_REFUND_REJECTED, "REJECTED");
    }

    @AfterEach
    void tearDown() {
        cleanFixtures();
    }

    /** 候选集必须排除已关店铺和商家被停用的店铺，并只取给定的 id */
    @Test
    void byIdsQueryReturnsOnlyRequestedOpenShops() {
        List<Long> ids = recommendationMapper
                .findOpenShopsByIds(List.of(SHOP_OPEN, SHOP_CLOSED, SHOP_DISABLED_MERCHANT))
                .stream().map(shop -> shop.getId()).toList();

        assertThat(ids).containsExactly(SHOP_OPEN);
    }

    @Test
    void productNamesSkipOffSaleAndDeletedProducts() {
        Map<Long, List<String>> byShop = recommendationMapper
                .findShopProductNames(List.of(SHOP_OPEN, SHOP_CLOSED))
                .stream()
                .collect(Collectors.groupingBy(
                        RecommendationMapper.ShopName::shopId,
                        Collectors.mapping(RecommendationMapper.ShopName::name, Collectors.toList())));

        assertThat(byShop.get(SHOP_OPEN))
                .containsExactlyInAnyOrder("红烧肉", "红烧排骨", "Cola 可乐");
    }

    @Test
    void categoriesAreScopedToRequestedShops() {
        assertThat(recommendationMapper.findShopCategories(List.of(SHOP_OPEN)))
                .containsExactly(new RecommendationMapper.ShopTarget(SHOP_OPEN, BIZ_CATEGORY_A));
    }

    /** 热度兜底的口径必须和复购一致：未支付、已取消和窗口外的订单都不算 */
    @Test
    void popularityCountsOnlyPaidUncancelledOrdersInsideTheWindow() {
        Map<Long, Integer> counts = recommendationMapper
                .findRecentPopularity(LocalDateTime.now().minusDays(7), 100)
                .stream()
                .collect(Collectors.toMap(
                        RecommendationMapper.ShopCount::shopId,
                        RecommendationMapper.ShopCount::times));

        assertThat(counts.get(SHOP_OPEN)).isEqualTo(1);
        assertThat(counts.get(SHOP_CLOSED)).isEqualTo(1);
    }

    // ===== 召回通道 =====

    @Test
    void businessCategoryRecallOnlyReturnsOpenShopIds() {
        assertThat(recommendationMapper.findOpenShopIdsByBusinessCategories(
                        List.of(BIZ_CATEGORY_A, BIZ_CATEGORY_B)))
                .containsExactly(SHOP_OPEN);
    }

    /** 关键词召回走 lower() + LIKE，大小写不敏感，且只认在售未删除的商品 */
    @Test
    void keywordRecallIsCaseInsensitiveAndSkipsDeadProducts() {
        assertThat(recommendationMapper.findOpenShopIdsByKeyword(List.of("cola")))
                .containsExactly(SHOP_OPEN);
        assertThat(recommendationMapper.findOpenShopIdsByKeyword(List.of("红烧")))
                .containsExactly(SHOP_OPEN);
        assertThat(recommendationMapper.findOpenShopIdsByKeyword(List.of("下架菜", "已删除菜")))
                .isEmpty();
    }

    /** 匹配范围必须和 ShopMapper.searchPage 一致：店名 / 经营范围 / 经营品类名 / 在售商品名 */
    @Test
    void keywordRecallMatchesEverythingSearchDoes() {
        assertThat(recommendationMapper.findOpenShopIdsByKeyword(List.of("营业中")))
                .containsExactly(SHOP_OPEN);
        assertThat(recommendationMapper.findOpenShopIdsByKeyword(List.of("推荐测试")))
                .containsExactly(SHOP_OPEN);
        // 品类名：搜索接口能靠它搜到店，召回少这一路就会漂移
        assertThat(recommendationMapper.findOpenShopIdsByKeyword(List.of("推荐测试品类A")))
                .containsExactly(SHOP_OPEN);
    }

    @Test
    void businessScopesAreScopedToRequestedShops() {
        assertThat(recommendationMapper.findShopBusinessScopes(List.of(SHOP_OPEN, SHOP_CLOSED)))
                .containsExactlyInAnyOrder(
                        new RecommendationMapper.ShopScope(SHOP_OPEN, "推荐测试"),
                        new RecommendationMapper.ShopScope(SHOP_CLOSED, "推荐测试"));
    }

    @Test
    void fallbackRecallReturnsOpenShopIdsOrderedById() {
        assertThat(recommendationMapper.findOpenShopIds(1)).containsExactly(SHOP_OPEN);
    }

    // ===== 负反馈 =====

    @Test
    void cancelledOrdersBecomePenalties() {
        List<RecommendationMapper.Affinity> penalties =
                recommendationMapper.findNegativeAffinity(USER_ID);

        assertThat(penalties).hasSize(1);
        assertThat(penalties.get(0).targetId()).isEqualTo(SHOP_OPEN);
        assertThat(penalties.get(0).times()).isEqualTo(1);
    }

    /** 只有退款通过的店才被排除，被驳回的退款不算 */
    @Test
    void onlyApprovedRefundsExcludeTheShop() {
        assertThat(recommendationMapper.findRefundedShopIds(USER_ID))
                .containsExactly(SHOP_OPEN);
    }

    private void merchant(long id, String name, boolean enabled) {
        jdbc.update("""
                INSERT INTO merchants(id, merchant_name, phone, password_hash, business_scope, enabled, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """, id, name, "1370000" + (id % 10000), "x".repeat(60), "推荐测试",
                enabled, Timestamp.valueOf(LocalDateTime.now()));
    }

    private void shop(long id, long merchantId, String name, String status) {
        jdbc.update("""
                INSERT INTO shops(id, merchant_id, shop_name, status)
                VALUES (?, ?, ?, ?)
                """, id, merchantId, name, status);
    }

    private void category(long id, long shopId, String name) {
        jdbc.update("""
                INSERT INTO categories(id, shop_id, name, sort_order)
                VALUES (?, ?, ?, ?)
                """, id, shopId, name, (int) (id % 10000));
    }

    private void product(long shopId, long categoryId, String name, String status, boolean deleted) {
        jdbc.update("""
                INSERT INTO products(shop_id, category_id, name, price, stock, status, deleted)
                VALUES (?, ?, ?, 10.00, 10, ?, ?)
                """, shopId, categoryId, name, status, deleted);
    }

    private void order(
            long id, String orderNo, long shopId, String paymentStatus, String status, LocalDateTime createdAt) {
        jdbc.update("""
                INSERT INTO orders(id, order_no, user_id, shop_id, total_amount, status, payment_status, created_at)
                VALUES (?, ?, ?, ?, 10.00, ?, ?, ?)
                """, id, orderNo, USER_ID, shopId, status, paymentStatus, Timestamp.valueOf(createdAt));
    }

    private void refund(long id, long orderId, String status) {
        jdbc.update("""
                INSERT INTO refund_requests(id, order_id, user_id, amount, reason, status, created_at)
                VALUES (?, ?, ?, 5.00, '推荐测试', ?, ?)
                """, id, orderId, USER_ID, status, Timestamp.valueOf(LocalDateTime.now()));
    }

    private void cleanFixtures() {
        jdbc.update("DELETE FROM refund_requests WHERE id >= 930000");
        jdbc.update("DELETE FROM orders WHERE id >= 930000");
        jdbc.update("DELETE FROM products WHERE shop_id >= 930000");
        jdbc.update("DELETE FROM shop_business_categories WHERE shop_id >= 930000");
        jdbc.update("DELETE FROM categories WHERE shop_id >= 930000");
        jdbc.update("DELETE FROM shops WHERE id >= 930000");
        jdbc.update("DELETE FROM merchants WHERE id >= 930000");
        jdbc.update("DELETE FROM business_categories WHERE id >= 930000");
        jdbc.update("DELETE FROM users WHERE id >= 930000");
    }
}
