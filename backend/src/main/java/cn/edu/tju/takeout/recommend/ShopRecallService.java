package cn.edu.tju.takeout.recommend;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;
import org.springframework.stereotype.Service;

/**
 * 召回：把全站店铺收敛成候选集合，只留下有可能拿到正分的店铺。
 *
 * <p>多路召回，任何一路命中都进候选：历史下单店、偏好品类下的店、关键词命中的店、
 * 近期热门店，候选不足时按 id 补齐。关键词那条覆盖店铺名 / 经营范围 / 在售商品名，
 * 和搜索接口的匹配范围一致。每路都对应打分公式里的一个正信号，所以这里返回的是
 * 「所有正分店铺」的超集——召回只决定谁有机会被排序，排前面交给打分和重排。
 *
 * <p>退款通过的店在这里剔除，不再进入后续任何阶段。
 */
@Service
class ShopRecallService {
    private final RecommendationMapper recommendationMapper;

    ShopRecallService(RecommendationMapper recommendationMapper) {
        this.recommendationMapper = recommendationMapper;
    }

    /**
     * @param popularShopIds 近段时间热门店，由调用方查好传入（打分阶段还要用同一份）
     * @param restrictTo 非 null 时只在该集合内召回；首页重排只关心当前页
     * @param fallbackWanted 候选不足这个数量时按 id 补齐，0 表示不补
     * @return 候选店铺 id，未过滤营业状态，由打分阶段统一过滤
     */
    Set<Long> recall(
            Preferences preferences,
            NegativeFeedback negative,
            Collection<Long> popularShopIds,
            Collection<Long> restrictTo,
            int fallbackWanted) {

        Set<Long> candidates = new LinkedHashSet<>();

        candidates.addAll(preferences.shops().keySet());

        if (!preferences.categories().isEmpty()) {
            candidates.addAll(recommendationMapper.findOpenShopIdsByBusinessCategories(
                    preferences.categories().keySet()));
        }

        if (!preferences.keywords().isEmpty()) {
            candidates.addAll(recommendationMapper.findOpenShopIdsByKeyword(
                    preferences.keywords().keySet()));
        }

        candidates.addAll(popularShopIds);

        candidates.removeAll(negative.excludedShops());

        if (restrictTo != null) {
            candidates.retainAll(restrictTo);
        }

        // 兜底补齐，保证「要几条就尽量给几条」
        if (fallbackWanted > 0 && candidates.size() < fallbackWanted) {
            candidates.addAll(recommendationMapper.findOpenShopIds(fallbackWanted));
        }

        return candidates;
    }
}
