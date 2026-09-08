package cn.edu.tju.takeout.shop;

import cn.edu.tju.takeout.auth.UserPrincipal;
import cn.edu.tju.takeout.common.ApiResponse;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/merchant/shop")
public class MerchantShopController {
    private final ShopService shopService;

    public MerchantShopController(ShopService shopService) {
        this.shopService = shopService;
    }

    @GetMapping
    public ApiResponse<ShopView> getMyShop(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.success(
                shopService.getMyShop(principal.userId())
        );
    }
}