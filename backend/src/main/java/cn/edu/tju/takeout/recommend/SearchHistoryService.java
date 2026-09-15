package cn.edu.tju.takeout.recommend;

import java.time.LocalDateTime;
import org.springframework.stereotype.Service;

@Service
public class SearchHistoryService {
    private static final int KEYWORD_MAX = 50;

    private final SearchHistoryMapper searchHistoryMapper;

    public SearchHistoryService(SearchHistoryMapper searchHistoryMapper) {
        this.searchHistoryMapper = searchHistoryMapper;
    }

    /**
     * 记录一次搜索。游客（userId 为 null）不记；与上一条完全相同的关键词也不记，
     * 否则搜索页从店铺返回时会重新搜一次，把同一个词灌满历史。
     */
    public void record(Long userId, String keyword) {
        if (userId == null) return;
        String word = keyword == null ? "" : keyword.trim();
        if (word.isEmpty()) return;
        if (word.length() > KEYWORD_MAX) word = word.substring(0, KEYWORD_MAX);
        if (word.equals(searchHistoryMapper.findLatestKeyword(userId))) return;
        searchHistoryMapper.insert(userId, word, LocalDateTime.now());
    }
}
