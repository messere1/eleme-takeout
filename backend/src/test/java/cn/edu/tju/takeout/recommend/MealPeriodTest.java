package cn.edu.tju.takeout.recommend;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalTime;
import org.junit.jupiter.api.Test;

class MealPeriodTest {
    @Test
    void periodsCoverEveryHourOfTheDay() {
        assertThat(MealPeriod.of(LocalTime.of(4, 59))).isEqualTo(MealPeriod.LATE_NIGHT);
        assertThat(MealPeriod.of(LocalTime.of(5, 0))).isEqualTo(MealPeriod.BREAKFAST);
        assertThat(MealPeriod.of(LocalTime.of(9, 59))).isEqualTo(MealPeriod.BREAKFAST);
        assertThat(MealPeriod.of(LocalTime.of(10, 0))).isEqualTo(MealPeriod.LUNCH);
        assertThat(MealPeriod.of(LocalTime.of(13, 59))).isEqualTo(MealPeriod.LUNCH);
        assertThat(MealPeriod.of(LocalTime.of(14, 0))).isEqualTo(MealPeriod.AFTERNOON_TEA);
        assertThat(MealPeriod.of(LocalTime.of(16, 59))).isEqualTo(MealPeriod.AFTERNOON_TEA);
        assertThat(MealPeriod.of(LocalTime.of(17, 0))).isEqualTo(MealPeriod.DINNER);
        assertThat(MealPeriod.of(LocalTime.of(20, 59))).isEqualTo(MealPeriod.DINNER);
        assertThat(MealPeriod.of(LocalTime.of(21, 0))).isEqualTo(MealPeriod.LATE_NIGHT);
        assertThat(MealPeriod.of(LocalTime.of(23, 59))).isEqualTo(MealPeriod.LATE_NIGHT);
    }

    @Test
    void breakfastFitsLightFoodButNotBarbecue() {
        assertThat(MealPeriod.fits("健康轻食", MealPeriod.BREAKFAST)).isTrue();
        assertThat(MealPeriod.fits("烧烤夜宵", MealPeriod.BREAKFAST)).isFalse();
        assertThat(MealPeriod.fits("烧烤夜宵", MealPeriod.LATE_NIGHT)).isTrue();
    }

    /** 顾客自建的品类不在映射表里，按中性处理；null 不能把 Map.of 查崩 */
    @Test
    void unknownOrMissingCategoryIsNeutral() {
        assertThat(MealPeriod.fits("顾客自建品类", MealPeriod.LUNCH)).isFalse();
        assertThat(MealPeriod.fits(null, MealPeriod.LUNCH)).isFalse();
        assertThat(MealPeriod.fits("快餐便当", null)).isFalse();
    }

    /** 演示数据里这几个品类也要覆盖，漏了它们会静默拿不到时段加分 */
    @Test
    void coversCategoriesPresentInDemoData() {
        assertThat(MealPeriod.fits("中式快餐", MealPeriod.BREAKFAST)).isTrue();
        assertThat(MealPeriod.fits("西式简餐", MealPeriod.DINNER)).isTrue();
        assertThat(MealPeriod.fits("奶茶甜品", MealPeriod.AFTERNOON_TEA)).isTrue();
        assertThat(MealPeriod.fits("地方菜系", MealPeriod.LUNCH)).isTrue();
    }
}
