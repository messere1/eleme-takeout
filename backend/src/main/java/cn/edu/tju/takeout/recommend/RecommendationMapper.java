package cn.edu.tju.takeout.recommend;

import cn.edu.tju.takeout.shop.Shop;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface RecommendationMapper {
    // 顾客在某店的有效订单（已支付且未取消）次数与最近一次时间
    @Select("""
        SELECT shop_id AS targetId, COUNT(*) AS times, MAX(created_at) AS lastAt
        FROM orders
        WHERE user_id = #{userId} AND payment_status = 'PAID' AND status <> 'CANCELLED'
        GROUP BY shop_id
        """)
    List<Affinity> findShopAffinity(Long userId);

    // 顾客对经营品类的偏好：订单 → 店铺 → 经营品类
    @Select("""
        SELECT sbc.business_category_id AS targetId, COUNT(*) AS times, MAX(o.created_at) AS lastAt
        FROM orders o
        JOIN shop_business_categories sbc ON sbc.shop_id = o.shop_id
        WHERE o.user_id = #{userId} AND o.payment_status = 'PAID' AND o.status <> 'CANCELLED'
        GROUP BY sbc.business_category_id
        """)
    List<Affinity> findCategoryAffinity(Long userId);

    // 只按店铺 id 取子集，没有任何全表版本：首页重排只关心当前这一页，
    // 全量查询会把整站商品拉进内存再逐个比对，随数据量增长迅速劣化。
    @Select("""
        <script>
        SELECT s.* FROM shops s
        JOIN merchants m ON m.id = s.merchant_id
        WHERE s.status = 'OPEN' AND m.enabled = TRUE
          AND s.id IN <foreach item="id" collection="ids" open="(" separator="," close=")">#{id}</foreach>
        </script>
        """)
    List<Shop> findOpenShopsByIds(@Param("ids") Collection<Long> ids);

    @Select("""
        <script>
        SELECT shop_id AS shopId, business_category_id AS targetId
        FROM shop_business_categories
        WHERE shop_id IN <foreach item="id" collection="ids" open="(" separator="," close=")">#{id}</foreach>
        </script>
        """)
    List<ShopTarget> findShopCategories(@Param("ids") Collection<Long> ids);

    @Select("""
        <script>
        SELECT DISTINCT shop_id AS shopId, name
        FROM products
        WHERE deleted = FALSE AND status = 'ON_SALE'
          AND shop_id IN <foreach item="id" collection="ids" open="(" separator="," close=")">#{id}</foreach>
        </script>
        """)
    List<ShopName> findShopProductNames(@Param("ids") Collection<Long> ids);

    // 近段时间各店订单数，作为无个人历史时的热门兜底，口径同 findShopAffinity。
    // 直接按订单量取前 N 而不是拉全城回来内存排序；ORDER BY 补 shop_id 对齐打分的同分兜底。
    @Select("""
        SELECT shop_id AS shopId, COUNT(*) AS times
        FROM orders
        WHERE created_at >= #{since} AND payment_status = 'PAID' AND status <> 'CANCELLED'
        GROUP BY shop_id
        ORDER BY COUNT(*) DESC, shop_id ASC
        LIMIT #{limit}
        """)
    List<ShopCount> findRecentPopularity(LocalDateTime since, int limit);

    // ===== 负反馈：都来自已有的 orders / refund_requests，不需要额外埋点 =====

    // 扣分档：取消过的店铺，只降权不拉黑
    @Select("""
        SELECT shop_id AS targetId, COUNT(*) AS times, MAX(created_at) AS lastAt
        FROM orders
        WHERE user_id = #{userId} AND status = 'CANCELLED'
        GROUP BY shop_id
        """)
    List<Affinity> findNegativeAffinity(Long userId);

    // 排除档：退款通过的店铺
    @Select("""
        SELECT DISTINCT o.shop_id
        FROM refund_requests r
        JOIN orders o ON o.id = r.order_id
        WHERE r.user_id = #{userId} AND r.status = 'APPROVED'
        """)
    List<Long> findRefundedShopIds(Long userId);

    // ===== 以下供召回阶段使用，都只返回店铺 id =====

    // 命中偏好经营品类的营业中店铺
    @Select("""
        <script>
        SELECT DISTINCT s.id FROM shops s
        JOIN merchants m ON m.id = s.merchant_id
        JOIN shop_business_categories sbc ON sbc.shop_id = s.id
        WHERE s.status = 'OPEN' AND m.enabled = TRUE
          AND sbc.business_category_id IN
          <foreach item="categoryId" collection="categoryIds" open="(" separator="," close=")">#{categoryId}</foreach>
        </script>
        """)
    List<Long> findOpenShopIdsByBusinessCategories(@Param("categoryIds") Collection<Long> categoryIds);

    // 关键词命中的营业中店铺。匹配范围和搜索接口一致：店铺名 / 经营范围 / 在售商品名。
    // 关键词含 % 或 _ 时 LIKE 只会多匹配，不会漏召；多召回一家 0 分店无副作用。
    @Select("""
        <script>
        SELECT DISTINCT s.id FROM shops s
        JOIN merchants m ON m.id = s.merchant_id
        LEFT JOIN products p ON p.shop_id = s.id AND p.deleted = FALSE AND p.status = 'ON_SALE'
        WHERE s.status = 'OPEN' AND m.enabled = TRUE
          AND
          <foreach item="keyword" collection="keywords" open="(" separator=" OR " close=")">
            (lower(s.shop_name) LIKE '%' || lower(#{keyword}) || '%'
             OR lower(m.business_scope) LIKE '%' || lower(#{keyword}) || '%'
             OR lower(p.name) LIKE '%' || lower(#{keyword}) || '%')
          </foreach>
        </script>
        """)
    List<Long> findOpenShopIdsByKeyword(@Param("keywords") Collection<String> keywords);

    // 店铺所属商家的经营范围，关键词打分要用
    @Select("""
        <script>
        SELECT s.id AS shopId, m.business_scope AS scope
        FROM shops s
        JOIN merchants m ON m.id = s.merchant_id
        WHERE s.id IN <foreach item="id" collection="ids" open="(" separator="," close=")">#{id}</foreach>
        </script>
        """)
    List<ShopScope> findShopBusinessScopes(@Param("ids") Collection<Long> ids);

    // 兜底补齐用：营业中店铺按 id 升序取前 N 个
    @Select("""
        SELECT s.id FROM shops s
        JOIN merchants m ON m.id = s.merchant_id
        WHERE s.status = 'OPEN' AND m.enabled = TRUE
        ORDER BY s.id
        LIMIT #{limit}
        """)
    List<Long> findOpenShopIds(@Param("limit") int limit);

    // 经营品类 id 到名称。表很小（内置八个品类 + 顾客自建的），餐段判断要用名字
    @Select("SELECT id, name FROM business_categories")
    List<BusinessCategory> findAllBusinessCategories();

    record BusinessCategory(Long id, String name) {}

    record ShopScope(Long shopId, String scope) {}

    /** targetId 是店铺 id 还是品类 id，取决于查询 */
    record Affinity(Long targetId, int times, LocalDateTime lastAt) {}
    record ShopTarget(Long shopId, Long targetId) {}
    record ShopName(Long shopId, String name) {}
    record ShopCount(Long shopId, int times) {}
}
