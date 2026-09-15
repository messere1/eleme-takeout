package cn.edu.tju.takeout.recommend;

import java.time.LocalDateTime;
import java.util.Map;

/** 一次推荐请求用到的全部输入，避免在召回/打分之间逐个传参 */
record RecommendationSignals(
        Preferences preferences,
        NegativeFeedback negative,
        Map<Long, Double> popularity,
        LocalDateTime now,
        MealPeriod mealPeriod) {
}
