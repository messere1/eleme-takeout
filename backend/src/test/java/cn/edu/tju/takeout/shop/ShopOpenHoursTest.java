package cn.edu.tju.takeout.shop;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalTime;
import org.junit.jupiter.api.Test;

/**
 * {@link Shop#isOpenAt} 的边界。下单链路和推荐都在用它判定「此刻能不能营业」，
 * 而 ShopManagementServiceTest 只覆盖了写入侧的校验，没覆盖判定本身。
 */
class ShopOpenHoursTest {
    @Test
    void missingBusinessHoursMeansNoRestriction() {
        assertThat(shop(null, null).isOpenAt(LocalTime.of(3, 0))).isTrue();
        assertThat(shop(LocalTime.of(9, 0), null).isOpenAt(LocalTime.of(3, 0))).isTrue();
    }

    /** 起止相同是非法配置，ShopService 在写入时已拦；读到旧数据时按打烊处理 */
    @Test
    void identicalOpeningAndClosingIsTreatedAsClosed() {
        assertThat(shop(LocalTime.of(9, 0), LocalTime.of(9, 0)).isOpenAt(LocalTime.NOON)).isFalse();
    }

    /** 闭店端点不含：21:00 打烊的店，21:00 已经不能下单 */
    @Test
    void closingInstantIsAlreadyClosed() {
        Shop shop = shop(LocalTime.of(9, 0), LocalTime.of(21, 0));

        assertThat(shop.isOpenAt(LocalTime.of(9, 0))).isTrue();
        assertThat(shop.isOpenAt(LocalTime.of(20, 59))).isTrue();
        assertThat(shop.isOpenAt(LocalTime.of(21, 0))).isFalse();
        assertThat(shop.isOpenAt(LocalTime.of(8, 59))).isFalse();
    }

    /** 营业到次日凌晨：闭店时间小于开店时间 */
    @Test
    void windowCrossingMidnightSpansBothSides() {
        Shop shop = shop(LocalTime.of(17, 0), LocalTime.of(2, 0));

        assertThat(shop.isOpenAt(LocalTime.of(23, 0))).isTrue();
        assertThat(shop.isOpenAt(LocalTime.of(1, 59))).isTrue();
        assertThat(shop.isOpenAt(LocalTime.of(2, 0))).isFalse();
        assertThat(shop.isOpenAt(LocalTime.of(12, 0))).isFalse();
        assertThat(shop.isOpenAt(LocalTime.of(3, 0))).isFalse();
    }

    private static Shop shop(LocalTime opening, LocalTime closing) {
        Shop shop = Shop.initiallyClosed(12L, "测试店铺");
        shop.updateBusinessHours(opening, closing);
        return shop;
    }
}
