package cn.edu.tju.takeout.recommend;

import java.util.Map;

/** 个人偏好：店铺复购、经营品类、搜索关键词。值都是带时间衰减的加权次数。 */
record Preferences(
        Map<Long, Double> shops,
        Map<Long, Double> categories,
        Map<String, Double> keywords) {
    static final Preferences EMPTY = new Preferences(Map.of(), Map.of(), Map.of());

    boolean isEmpty() {
        return shops.isEmpty() && categories.isEmpty() && keywords.isEmpty();
    }
}
