package cn.edu.tju.takeout.recommend;

import cn.edu.tju.takeout.shop.Shop;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Component;

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
