package cn.edu.tju.takeout.shop;

import cn.edu.tju.takeout.auth.UserPrincipal;
import cn.edu.tju.takeout.common.ApiResponse;
import cn.edu.tju.takeout.catalog.*;
import cn.edu.tju.takeout.common.BusinessException;
import jakarta.validation.Valid;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/merchant/shop")
public class MerchantShopController {
    private final ShopService shopService;
    private final ShopMapper shops;
    private final BusinessCategoryMapper categories;

    public MerchantShopController(ShopService shopService, ShopMapper shops, BusinessCategoryMapper categories) {
        this.shopService = shopService;
        this.shops=shops;this.categories=categories;
    }

    @GetMapping
    public ApiResponse<ShopView> getMyShop(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.success(
                shopService.getMyShop(principal.userId())
        );
    }

    @GetMapping("/business-categories")
    public ApiResponse<List<BusinessCategory>> categories(@AuthenticationPrincipal UserPrincipal principal){
        Shop shop=shops.findByMerchantId(principal.userId()).orElseThrow(()->new BusinessException(HttpStatus.NOT_FOUND,"RESOURCE_NOT_FOUND","店铺不存在"));
        return ApiResponse.success(categories.findByShopId(shop.getId()));
    }

    @PutMapping("/business-categories") @Transactional
    public ApiResponse<List<BusinessCategory>> updateCategories(@AuthenticationPrincipal UserPrincipal principal,@Valid @RequestBody ShopBusinessCategoriesRequest request){
        Shop shop=shops.findByMerchantId(principal.userId()).orElseThrow(()->new BusinessException(HttpStatus.NOT_FOUND,"RESOURCE_NOT_FOUND","店铺不存在"));
        List<Long> ids=request.categoryIds().stream().filter(Objects::nonNull).distinct().toList();
        if(ids.isEmpty()||ids.size()>3)throw new BusinessException(HttpStatus.BAD_REQUEST,"VALIDATION_ERROR","请选择一至三个经营品类");
        if(ids.stream().anyMatch(id->categories.countEnabledById(id)==0))throw new BusinessException(HttpStatus.BAD_REQUEST,"VALIDATION_ERROR","经营品类不存在或已停用");
        categories.deleteByShopId(shop.getId());ids.forEach(id->categories.linkShop(shop.getId(),id));
        return ApiResponse.success(categories.findByShopId(shop.getId()));
    }
}
