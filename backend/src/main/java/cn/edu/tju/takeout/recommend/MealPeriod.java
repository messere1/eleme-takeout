package cn.edu.tju.takeout.recommend;

import java.time.LocalTime;
import java.util.Map;
import java.util.Set;

/**
 * 餐段。同一个顾客在早上和半夜打开首页，该看到的店完全不一样：
 * 早上推烧烤店、凌晨推轻食店都是错的。
 */
enum MealPeriod {
    BREAKFAST,
    LUNCH,
    AFTERNOON_TEA,
    DINNER,
    LATE_NIGHT;

    /** 21:00~次日 05:00 归夜宵，所以判断按上界显式分段，不能只写下界 */
    static MealPeriod of(LocalTime time) {
        int hour = time.getHour();
        if (hour >= 5 && hour < 10) return BREAKFAST;
        if (hour >= 10 && hour < 14) return LUNCH;
        if (hour >= 14 && hour < 17) return AFTERNOON_TEA;
        if (hour >= 17 && hour < 21) return DINNER;
        return LATE_NIGHT;
    }

    /**
     * 经营品类名 → 适合的餐段。覆盖库里实际存在的十二个品类：内置的八个，
     * 加上演示数据里的中式快餐 / 西式简餐 / 奶茶甜品 / 地方菜系。
     * 顾客自建的品类不在表里，一律按中性处理（不加分也不减分）。
     */
    private static final Map<String, Set<MealPeriod>> BY_CATEGORY = Map.ofEntries(
            Map.entry("快餐便当", Set.of(BREAKFAST, LUNCH, DINNER)),
            Map.entry("奶茶饮品", Set.of(BREAKFAST, AFTERNOON_TEA, LATE_NIGHT)),
            Map.entry("小吃炸物", Set.of(AFTERNOON_TEA, LATE_NIGHT)),
            Map.entry("汉堡披萨", Set.of(LUNCH, AFTERNOON_TEA, DINNER, LATE_NIGHT)),
            Map.entry("日韩料理", Set.of(LUNCH, DINNER)),
            Map.entry("烧烤夜宵", Set.of(DINNER, LATE_NIGHT)),
            Map.entry("甜品烘焙", Set.of(AFTERNOON_TEA, LATE_NIGHT)),
            Map.entry("健康轻食", Set.of(BREAKFAST, LUNCH)),
            Map.entry("中式快餐", Set.of(BREAKFAST, LUNCH, DINNER)),
            Map.entry("西式简餐", Set.of(LUNCH, DINNER)),
            Map.entry("奶茶甜品", Set.of(AFTERNOON_TEA, LATE_NIGHT)),
            Map.entry("地方菜系", Set.of(LUNCH, DINNER)));

    static boolean fits(String categoryName, MealPeriod period) {
        // BY_CATEGORY 由 Map.of 构造，查 null 键会直接抛 NPE
        if (categoryName == null || period == null) return false;
        Set<MealPeriod> periods = BY_CATEGORY.get(categoryName);
        return periods != null && periods.contains(period);
    }
}
