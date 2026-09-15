package cn.edu.tju.takeout.recommend;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalTime;
import org.junit.jupiter.api.Test;

class ServingTimeTest {
    @Test
    void missingBusinessHoursMeansNoRestriction() {
        assertThat(ServingTime.isOpenAt(null, null, LocalTime.of(3, 0))).isTrue();
        assertThat(ServingTime.isOpenAt(LocalTime.of(9, 0), null, LocalTime.of(3, 0))).isTrue();
    }

    /** 起止相同视为全天营业，否则会被当成 0 长度窗口永远打烊 */
    @Test
    void identicalOpeningAndClosingMeansAllDay() {
        assertThat(ServingTime.isOpenAt(LocalTime.of(9, 0), LocalTime.of(9, 0), LocalTime.of(3, 0)))
                .isTrue();
    }

    @Test
    void normalWindowIsInclusiveOnBothEnds() {
        LocalTime opening = LocalTime.of(9, 0);
        LocalTime closing = LocalTime.of(21, 0);

        assertThat(ServingTime.isOpenAt(opening, closing, LocalTime.of(9, 0))).isTrue();
        assertThat(ServingTime.isOpenAt(opening, closing, LocalTime.of(15, 0))).isTrue();
        assertThat(ServingTime.isOpenAt(opening, closing, LocalTime.of(21, 0))).isTrue();
        assertThat(ServingTime.isOpenAt(opening, closing, LocalTime.of(8, 59))).isFalse();
        assertThat(ServingTime.isOpenAt(opening, closing, LocalTime.of(21, 1))).isFalse();
    }

    /** 营业到次日凌晨的店，闭店时间小于开店时间 */
    @Test
    void windowCrossingMidnightSpansBothSides() {
        LocalTime opening = LocalTime.of(17, 0);
        LocalTime closing = LocalTime.of(2, 0);

        assertThat(ServingTime.isOpenAt(opening, closing, LocalTime.of(23, 0))).isTrue();
        assertThat(ServingTime.isOpenAt(opening, closing, LocalTime.of(1, 0))).isTrue();
        assertThat(ServingTime.isOpenAt(opening, closing, LocalTime.of(12, 0))).isFalse();
        assertThat(ServingTime.isOpenAt(opening, closing, LocalTime.of(3, 0))).isFalse();
    }
}
