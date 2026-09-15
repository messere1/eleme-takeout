package cn.edu.tju.takeout.recommend;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SearchHistoryServiceTest {
    @Mock private SearchHistoryMapper searchHistoryMapper;
    private SearchHistoryService service;

    @BeforeEach
    void setUp() {
        service = new SearchHistoryService(searchHistoryMapper);
    }

    @Test
    void guestSearchIsNotRecorded() {
        service.record(null, "奶茶");

        verifyNoInteractions(searchHistoryMapper);
    }

    @Test
    void blankKeywordIsNotRecorded() {
        service.record(7L, null);
        service.record(7L, "");
        service.record(7L, "   ");

        verifyNoInteractions(searchHistoryMapper);
    }

    @Test
    void keywordIsTrimmedBeforeStoring() {
        when(searchHistoryMapper.findLatestKeyword(7L)).thenReturn("旧关键词");

        service.record(7L, "  奶茶  ");

        verify(searchHistoryMapper).insert(eq(7L), eq("奶茶"), any(LocalDateTime.class));
    }

    /** 列宽是 VARCHAR(50)，超过部分必须在进库前截断，否则写入会失败 */
    @Test
    void keywordLongerThanColumnIsTruncatedTo50Chars() {
        when(searchHistoryMapper.findLatestKeyword(7L)).thenReturn(null);

        service.record(7L, "奶".repeat(60));

        ArgumentCaptor<String> stored = ArgumentCaptor.forClass(String.class);
        verify(searchHistoryMapper).insert(eq(7L), stored.capture(), any(LocalDateTime.class));
        assertThat(stored.getValue()).hasSize(50);
    }

    /** 搜索页从店铺返回时会重新搜一次，连续重复的关键词不能把历史灌满 */
    @Test
    void consecutiveRepeatOfSameKeywordIsNotRecordedTwice() {
        when(searchHistoryMapper.findLatestKeyword(7L)).thenReturn("奶茶");

        service.record(7L, "奶茶");

        verify(searchHistoryMapper, never()).insert(any(), anyString(), any(LocalDateTime.class));
    }

    /** 去重只看紧邻的上一条，中间隔了别的词就要重新记一次 */
    @Test
    void sameKeywordIsRecordedAgainWhenAnotherKeywordCameInBetween() {
        when(searchHistoryMapper.findLatestKeyword(7L)).thenReturn("麻辣烫");

        service.record(7L, "奶茶");

        verify(searchHistoryMapper).insert(eq(7L), eq("奶茶"), any(LocalDateTime.class));
    }
}
