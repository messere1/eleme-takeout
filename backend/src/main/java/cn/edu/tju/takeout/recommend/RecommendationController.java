package cn.edu.tju.takeout.recommend;

import cn.edu.tju.takeout.auth.UserPrincipal;
import cn.edu.tju.takeout.common.ApiResponse;
import cn.edu.tju.takeout.shop.ShopView;
import java.util.List;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/recommendations")
public class RecommendationController {
    private final RecommendationService recommendationService;

    public RecommendationController(RecommendationService recommendationService) {
        this.recommendationService = recommendationService;
    }

    /** 未登录（principal 为 null）时只按热度推荐，不涉及个人历史 */
    @GetMapping("/shops")
    public ApiResponse<List<ShopView>> recommendedShops(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(defaultValue = "6") Integer limit) {
        return ApiResponse.success(recommendationService.recommendShops(
                principal == null ? null : principal.userId(), limit));
    }
}
