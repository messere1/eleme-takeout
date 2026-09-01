package cn.edu.tju.takeout.shop;

import cn.edu.tju.takeout.auth.UserPrincipal;
import cn.edu.tju.takeout.common.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/shops")
public class ShopController {
    private final ShopService shopService;
    public ShopController(ShopService shopService) { this.shopService = shopService; }

    @PatchMapping("/{shopId}/status")
    public ApiResponse<ShopView> changeStatus(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long shopId,
            @Valid @RequestBody ShopStatusRequest request) {
        return ApiResponse.success(shopService.changeStatus(principal.userId(), shopId, request));
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
}
