package cn.edu.tju.takeout.recommend;

import java.util.Map;
import java.util.Set;

/**
 * 负反馈。取消订单只扣分：取消常和出餐慢、临时改主意有关，不代表这家店不好。
 * 退款通过则直接排除：已经拿回过钱，属最强否定信号。
 */
record NegativeFeedback(Map<Long, Double> penalties, Set<Long> excludedShops) {
    static final NegativeFeedback EMPTY = new NegativeFeedback(Map.of(), Set.of());

    boolean isEmpty() {
        return penalties.isEmpty() && excludedShops.isEmpty();
    }
}
