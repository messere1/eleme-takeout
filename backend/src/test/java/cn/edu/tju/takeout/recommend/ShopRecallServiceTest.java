package cn.edu.tju.takeout.recommend;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ShopRecallServiceTest {
    private static final Preferences PREFERENCES = new Preferences(
            Map.of(1L, 1.0), Map.of(3L, 1.0), Map.of("煎饼", 1.0));

    private final RecommendationMapper mapper = mock(RecommendationMapper.class);
    private final ShopRecallService recall = new ShopRecallService(mapper);

    @BeforeEach
    void setUp() {
        when(mapper.findOpenShopIdsByBusinessCategories(anyCollection())).thenReturn(List.of());
        when(mapper.findOpenShopIdsByKeyword(anyCollection())).thenReturn(List.of());
        when(mapper.findOpenShopIds(anyInt())).thenReturn(List.of());
    }

    /** 四路召回合并：历史店 1 + 品类店 5 + 关键词店 6 + 热门店 7 */
    @Test
    void mergesEveryChannel() {
        when(mapper.findOpenShopIdsByBusinessCategories(anyCollection())).thenReturn(List.of(5L));
        when(mapper.findOpenShopIdsByKeyword(anyCollection())).thenReturn(List.of(6L));

        Set<Long> ids = recall.recall(PREFERENCES, NegativeFeedback.EMPTY, Set.of(7L), null, 0);

        assertThat(ids).containsExactlyInAnyOrder(1L, 5L, 6L, 7L);
    }

    @Test
    void refundedShopsAreDroppedFromCandidates() {
        Set<Long> ids = recall.recall(
                PREFERENCES, new NegativeFeedback(Map.of(), Set.of(1L)), Set.of(), null, 0);

        assertThat(ids).isEmpty();
    }

    /** 首页重排只关心当前页：召回结果必须收敛到页面上的店铺 */
    @Test
    void restrictToKeepsOnlyRequestedShops() {
        Set<Long> ids = recall.recall(
                PREFERENCES, NegativeFeedback.EMPTY, Set.of(), Set.of(1L, 99L), 0);

        assertThat(ids).containsExactly(1L);
    }

    /** 没有偏好时靠兜底补齐，保证首页不出现空位 */
    @Test
    void fallbackFillsWhenCandidatesAreTooFew() {
        when(mapper.findOpenShopIds(10)).thenReturn(List.of(1L, 2L, 3L));

        Set<Long> ids = recall.recall(Preferences.EMPTY, NegativeFeedback.EMPTY, Set.of(), null, 10);

        assertThat(ids).containsExactly(1L, 2L, 3L);
    }

    @Test
    void fallbackIsSkippedWhenCandidatesAlreadyEnough() {
        when(mapper.findOpenShopIds(anyInt())).thenReturn(List.of(99L));

        Set<Long> ids = recall.recall(PREFERENCES, NegativeFeedback.EMPTY, Set.of(), null, 1);

        assertThat(ids).doesNotContain(99L);
    }
}
