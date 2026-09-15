package cn.edu.tju.takeout.recommend;

import cn.edu.tju.takeout.shop.Shop;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * 重排：打分之后施加业务规则。打分回答「哪家店更相关」，重排回答「这一屏列表合不合理」。
 *
 * <p>两条规则：同一经营品类最多连续 {@link #CATEGORY_RUN} 家；每 {@link #REPEAT_RUN} 家复购店
 * 之后插一家探索店（约 70% 复购 / 30% 探索）。前者避免整屏都是一个品类，后者避免
 * 「点过一次川菜就天天推川菜」，也给没被历史覆盖的店留曝光位。
 *
 * <p>约束无法同时满足时退回分数优先，不会因为规则把店铺丢掉。
 */
@Component
class RecommendationRerank {
    private static final int REPEAT_RUN = 2;
    private static final int CATEGORY_RUN = 2;

    List<ScoredShop> apply(List<ScoredShop> ranked, Preferences preferences, ShopFacts facts) {
        if (ranked.size() < 2) return ranked;

        List<ScoredShop> pending = new ArrayList<>(ranked);
        List<ScoredShop> result = new ArrayList<>(ranked.size());
        int repeatsInARow = 0;

        while (!pending.isEmpty()) {
            ScoredShop next = pick(pending, result, facts, preferences, repeatsInARow >= REPEAT_RUN);
            pending.remove(next);
            result.add(next);
            repeatsInARow = isRepeat(next, preferences) ? repeatsInARow + 1 : 0;
        }
        return result;
    }

    /** 按分数顺序挑第一个不违反约束的；全都违反时退回原第一名 */
    private static ScoredShop pick(
            List<ScoredShop> pending,
            List<ScoredShop> chosen,
            ShopFacts facts,
            Preferences preferences,
            boolean exploreDue) {
        for (ScoredShop candidate : pending) {
            if (exploreDue && isRepeat(candidate, preferences)) continue;
            if (extendsCategoryRun(candidate, chosen, facts)) continue;
            return candidate;
        }
        return pending.get(0);
    }

    private static boolean isRepeat(ScoredShop item, Preferences preferences) {
        return preferences.shops().containsKey(item.shop().getId());
    }

    /** 加进去会让同一品类连成 CATEGORY_RUN 家以上，就该往后放 */
    private static boolean extendsCategoryRun(
            ScoredShop candidate, List<ScoredShop> chosen, ShopFacts facts) {
        if (chosen.size() < CATEGORY_RUN) return false;
        for (int i = chosen.size() - CATEGORY_RUN; i < chosen.size(); i++) {
            if (!sharesCategory(chosen.get(i).shop(), candidate.shop(), facts)) return false;
        }
        return true;
    }

    /** 两家店只要有一个共同经营品类就算同品类 */
    private static boolean sharesCategory(Shop a, Shop b, ShopFacts facts) {
        Set<Long> categoriesA = facts.categoriesByShop().getOrDefault(a.getId(), Set.of());
        Set<Long> categoriesB = facts.categoriesByShop().getOrDefault(b.getId(), Set.of());
        if (categoriesA.isEmpty() || categoriesB.isEmpty()) return false;
        for (Long categoryId : categoriesA) {
            if (categoriesB.contains(categoryId)) return true;
        }
        return false;
    }
}
