package cn.edu.tju.takeout.shop;

import cn.edu.tju.takeout.auth.UserPrincipal;
import cn.edu.tju.takeout.common.ApiResponse;
import cn.edu.tju.takeout.recommend.RecommendationService;
import cn.edu.tju.takeout.recommend.SearchHistoryService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/shops")
public class ShopController {
    private final ShopService shopService;
    private final SearchHistoryService searchHistoryService;
    private final RecommendationService recommendationService;

    public ShopController(
            ShopService shopService,
            SearchHistoryService searchHistoryService,
            RecommendationService recommendationService) {
        this.shopService = shopService;
        this.searchHistoryService = searchHistoryService;
        this.recommendationService = recommendationService;
    }

    @PatchMapping("/{shopId}/status")
    public ApiResponse<ShopView> changeStatus(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long shopId,
            @Valid @RequestBody ShopStatusRequest request) {
        return ApiResponse.success(shopService.changeStatus(principal.userId(), shopId, request));
    }

    @GetMapping("/search")
    public ApiResponse<ShopPage> searchShops(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size,
            @RequestParam String keyword) {
        // 先搜再记：searchShops 会校验页码、每页条数和空关键词并抛 400，
        // 记在它前面的话，非法请求也会把关键词写进历史（推荐算法的偏好信号）。
        ShopPage result = shopService.searchShops(page, size, keyword);
        // 该接口对游客开放，登录用户的关键词才记入历史
        Long customerId = customerId(principal);
        if (customerId != null) {
            searchHistoryService.record(customerId, keyword);
        }
        return ApiResponse.success(result);
    }

    @GetMapping("/{shopId}")
    public ApiResponse<ShopView> getShop(@PathVariable Long shopId) {
        return ApiResponse.success(shopService.getShop(shopId));
    }

    @PatchMapping("/{shopId}")
    public ApiResponse<ShopView> updateShop(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long shopId,
            @Valid @RequestBody ShopUpdateRequest request) {
        return ApiResponse.success(shopService.updateShop(principal.userId(), shopId, request));
    }

    @GetMapping
    public ApiResponse<ShopPage> listShops(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size,
            @RequestParam(required = false) String businessScope,
            @RequestParam(required = false) Long businessCategoryId) {

        ShopPage result = shopService.listShops(page, size, businessScope, businessCategoryId);
        // 登录顾客看到的是按偏好重排过的顺序；游客和没有历史的顾客维持原顺序
        return ApiResponse.success(recommendationService.reorderByPreference(
                customerId(principal), result));
    }

    @PatchMapping("/{shopId}/business-hours")
    public ApiResponse<ShopView> updateBusinessHours(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long shopId,
            @Valid @RequestBody ShopBusinessHoursRequest request){
        return ApiResponse.success(
            shopService.updateBusinessHours(
                    principal.userId(),
                    shopId,
                    request          
                )
            );
    }

    private static Long customerId(UserPrincipal principal) {
        return principal != null && "CUSTOMER".equals(principal.role())
                ? principal.userId()
                : null;
    }
}
