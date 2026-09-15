package cn.edu.tju.takeout.recommend;

import cn.edu.tju.takeout.shop.Shop;
import cn.edu.tju.takeout.shop.ShopPage;
import cn.edu.tju.takeout.shop.ShopView;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;

/**
 * 店铺推荐编排：召回 → 打分 → 重排。本类只管流水线顺序和数据装载，
 * 三段规则分别在 {@link ShopRecallService}、{@link RecommendationScoring}、{@link RecommendationRerank}。
 *
 * <p>不使用模型或离线任务：把顾客的下单历史与搜索历史折算成偏好权重，
 * 对候选店铺加权排序。没有任何历史时退回全站热度。
 */
@Service
public class RecommendationService {
    private static final int DEFAULT_LIMIT = 6;
    private static final int MAX_LIMIT = 50;
    private static final int KEYWORD_LIMIT = 20;
    private static final int POPULARITY_DAYS = 7;

    private final RecommendationMapper recommendationMapper;
    private final SearchHistoryMapper searchHistoryMapper;
    private final ShopRecallService recallService;
    private final RecommendationScoring scoring;
    private final RecommendationRerank rerank;

    public RecommendationService(
            RecommendationMapper recommendationMapper,
            SearchHistoryMapper searchHistoryMapper,
            ShopRecallService recallService,
            RecommendationScoring scoring,
            RecommendationRerank rerank) {
        this.recommendationMapper = recommendationMapper;
        this.searchHistoryMapper = searchHistoryMapper;
        this.recallService = recallService;
        this.scoring = scoring;
        this.rerank = rerank;
    }

    public List<ShopView> recommendShops(Long userId, Integer limit) {
        int size = limit == null ? DEFAULT_LIMIT : limit;
        if (size < 1) size = 1;
        if (size > MAX_LIMIT) size = MAX_LIMIT;

        LocalDateTime now = LocalDateTime.now();
        Preferences preferences = readPreferences(userId, now);
        NegativeFeedback negative = readNegativeFeedback(userId, now);
        boolean coldStart = preferences.isEmpty();
        Map<Long, Double> popularity = coldStart ? readPopularity(now, size) : Map.of();

        RecommendationSignals signals = new RecommendationSignals(
                preferences, negative, popularity, now, MealPeriod.of(now.toLocalTime()));

        // 兜底池固定用 MAX_LIMIT，不随请求的 limit 变。否则池子大小跟着 limit 走，
        // 高分店可能只在 limit 大时才进池，导致「前 4 条」不是「前 6 条」的前缀。
        return pipeline(signals, null, MAX_LIMIT).stream()
                .limit(size)
                .map(item -> ShopView.from(item.shop()))
                .toList();
    }

    /**
     * 按偏好重排一页店铺：用户偏好命中的排前面，其余保持原有相对顺序跟在后面。
     * 没有偏好（游客、或没有下单也没有搜索历史）时不动顺序。
     */
    public ShopPage reorderByPreference(Long userId, ShopPage page) {
        if (userId == null || page.items() == null || page.items().size() < 2) return page;

        LocalDateTime now = LocalDateTime.now();
        Preferences preferences = readPreferences(userId, now);
        if (preferences.isEmpty()) return page;
        NegativeFeedback negative = readNegativeFeedback(userId, now);

        RecommendationSignals signals = new RecommendationSignals(
                preferences, negative, Map.of(), now, MealPeriod.of(now.toLocalTime()));

        // 只给这一页的店铺打分：单店得分不依赖候选集（热度项此时不参与），
        // 缩小候选集不改变这一页内部的相对顺序，却省掉了整站商品的全量扫描。
        List<Long> pageShopIds = page.items().stream().map(ShopView::id).toList();
        List<ScoredShop> ranked = pipeline(signals, pageShopIds, 0);

        // 只有被偏好命中的店铺才给名次；ranked 已经过重排且按分数降序，取到第一个 0 分即可停。
        // 若给 0 分的店铺也排名次，它们会被同分兜底（按 id 升序）反过来。
        Map<Long, Integer> rankOf = new HashMap<>();
        int position = 0;
        for (ScoredShop item : ranked) {
            if (item.score() <= 0) break;
            rankOf.put(item.shop().getId(), position);
            position += 1;
        }

        List<ShopView> sorted = new ArrayList<>(page.items());
        // List.sort 是稳定排序，没被推荐命中的店铺相对顺序不变
        sorted.sort(Comparator.comparingInt(view -> rankOf.getOrDefault(view.id(), Integer.MAX_VALUE)));

        return new ShopPage(sorted, page.page(), page.size(), page.total(), page.totalPages());
    }

    /** 流水线本体：召回 → 打分 → 重排 */
    private List<ScoredShop> pipeline(
            RecommendationSignals signals, Collection<Long> restrictTo, int fallbackWanted) {

        Set<Long> candidateIds = recallService.recall(
                signals.preferences(), signals.negative(), signals.popularity().keySet(),
                restrictTo, fallbackWanted);

        // 营业时间之外的店按打烊处理，和 status 一样属于硬过滤。
        // 判定复用 Shop.isOpenAt，与下单链路的校验同一口径，否则会出现「推了却下不了单」。
        LocalTime timeOfDay = signals.now().toLocalTime();
        List<Shop> candidates = recommendationMapper.findOpenShopsByIds(candidateIds).stream()
                .filter(shop -> shop.isOpenAt(timeOfDay))
                .toList();
        if (candidates.isEmpty()) return List.of();

        ShopFacts facts = loadFacts(candidateIds);
        List<ScoredShop> ranked = scoring.score(candidates, facts, signals);
        return rerank.apply(ranked, signals.preferences(), facts);
    }

    private ShopFacts loadFacts(Collection<Long> candidateIds) {
        Map<Long, String> categoryNames = new HashMap<>();
        for (RecommendationMapper.BusinessCategory row
                : recommendationMapper.findAllBusinessCategories()) {
            categoryNames.put(row.id(), row.name());
        }

        Map<Long, String> businessScopes = new HashMap<>();
        for (RecommendationMapper.ShopScope row
                : recommendationMapper.findShopBusinessScopes(candidateIds)) {
            businessScopes.put(row.shopId(), row.scope());
        }

        return new ShopFacts(
                groupTargets(recommendationMapper.findShopCategories(candidateIds)),
                groupNames(recommendationMapper.findShopProductNames(candidateIds)),
                businessScopes,
                categoryNames);
    }

    /** 个人偏好：店铺复购、经营品类、搜索关键词，都按时间衰减 */
    private Preferences readPreferences(Long userId, LocalDateTime now) {
        if (userId == null) return Preferences.EMPTY;

        Map<Long, Double> shops = new HashMap<>();
        for (RecommendationMapper.Affinity stat : recommendationMapper.findShopAffinity(userId)) {
            shops.merge(stat.targetId(), RecommendationScoring.decay(stat.times(), stat.lastAt(), now), Double::sum);
        }

        Map<Long, Double> categories = new HashMap<>();
        for (RecommendationMapper.Affinity stat : recommendationMapper.findCategoryAffinity(userId)) {
            categories.merge(stat.targetId(), RecommendationScoring.decay(stat.times(), stat.lastAt(), now), Double::sum);
        }

        Map<String, Double> keywords = new HashMap<>();
        for (SearchHistoryMapper.KeywordStat stat : searchHistoryMapper.findTopKeywords(userId, KEYWORD_LIMIT)) {
            String word = stat.keyword() == null ? "" : stat.keyword().trim();
            if (!word.isEmpty()) {
                keywords.merge(word, RecommendationScoring.decay(stat.times(), stat.lastAt(), now), Double::sum);
            }
        }

        return new Preferences(shops, categories, keywords);
    }

    /** 负反馈：退款通过的店排除，取消过的店扣分。数据都在 orders / refund_requests 里，不用额外埋点 */
    private NegativeFeedback readNegativeFeedback(Long userId, LocalDateTime now) {
        if (userId == null) return NegativeFeedback.EMPTY;

        Map<Long, Double> penalties = new HashMap<>();
        Set<Long> excluded = new HashSet<>();
        for (RecommendationMapper.Affinity stat : recommendationMapper.findNegativeAffinity(userId)) {
            penalties.merge(stat.targetId(), RecommendationScoring.decay(stat.times(), stat.lastAt(), now), Double::sum);
        }
        excluded.addAll(recommendationMapper.findRefundedShopIds(userId));

        return new NegativeFeedback(penalties, excluded);
    }

    private Map<Long, Double> readPopularity(LocalDateTime now, int wanted) {
        return RecommendationScoring.normalizePopularity(
                recommendationMapper.findRecentPopularity(now.minusDays(POPULARITY_DAYS), wanted));
    }

    private static Map<Long, Set<Long>> groupTargets(List<RecommendationMapper.ShopTarget> rows) {
        Map<Long, Set<Long>> grouped = new HashMap<>();
        for (RecommendationMapper.ShopTarget row : rows) {
            grouped.computeIfAbsent(row.shopId(), key -> new HashSet<>()).add(row.targetId());
        }
        return grouped;
    }

    private static Map<Long, List<String>> groupNames(List<RecommendationMapper.ShopName> rows) {
        Map<Long, List<String>> grouped = new HashMap<>();
        for (RecommendationMapper.ShopName row : rows) {
            if (row.name() == null || row.name().isEmpty()) continue;
            grouped.computeIfAbsent(row.shopId(), key -> new ArrayList<>()).add(row.name());
        }
        return grouped;
    }
}
