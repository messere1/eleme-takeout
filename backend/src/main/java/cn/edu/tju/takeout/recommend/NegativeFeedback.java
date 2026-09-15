package cn.edu.tju.takeout.recommend;

import java.util.Map;
import java.util.Set;

record NegativeFeedback(Map<Long, Double> penalties, Set<Long> excludedShops) {
    static final NegativeFeedback EMPTY = new NegativeFeedback(Map.of(), Set.of());

    boolean isEmpty() {
        return penalties.isEmpty() && excludedShops.isEmpty();
    }
}
