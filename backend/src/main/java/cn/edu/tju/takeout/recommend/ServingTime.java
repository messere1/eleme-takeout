package cn.edu.tju.takeout.recommend;

import java.time.LocalTime;

/** 营业时间判断。两个列都可空，没配就当作不限时段。 */
final class ServingTime {
    private ServingTime() {}

    static boolean isOpenAt(LocalTime opening, LocalTime closing, LocalTime now) {
        if (opening == null || closing == null) return true;
        // 起止相同视为全天营业
        if (opening.equals(closing)) return true;

        // 营业到次日凌晨的店（如 17:00~02:00），闭店时间小于开店时间
        return opening.isBefore(closing)
                ? !now.isBefore(opening) && !now.isAfter(closing)
                : !now.isBefore(opening) || !now.isAfter(closing);
    }
}
