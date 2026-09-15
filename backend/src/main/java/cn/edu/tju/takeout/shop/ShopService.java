package cn.edu.tju.takeout.shop;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import cn.edu.tju.takeout.common.BusinessException;

@Service
public class ShopService {

    private final ShopMapper shopMapper;

    public ShopService(ShopMapper shopMapper) {
        this.shopMapper=shopMapper;
    }

    public ShopView getMyShop(Long merchantId){
        Shop shop=shopMapper.findByMerchantId(merchantId)
            .orElseThrow(()->new BusinessException(
                HttpStatus.NOT_FOUND,
                "RESOURCE_NOT_FOUND",
                "当前商家尚未创建店铺"
            )
        );
        return ShopView.from(shop);
    }

    public ShopView changeStatus(Long merchantId, Long shopId, ShopStatusRequest request) {
        Shop shop=shopMapper.findById(shopId).orElseThrow(()->
        new BusinessException(
            HttpStatus.NOT_FOUND,
            "RESOURCE_NOT_FOUND",
            "店铺不存在")
        );

        if(!shop.getMerchantId().equals(merchantId)){
            throw new BusinessException(
                HttpStatus.FORBIDDEN,
                "FORBIDDEN",
            "无权修改该店铺"
            );
        }

        shop.changeStatus(request.status());
        shopMapper.updateStatus(shop);
        return ShopView.from(shop);

    }

    public ShopView getShop(Long shopId) {
        Shop shop=shopMapper.findById(shopId).orElseThrow(()->
        new BusinessException(
            HttpStatus.NOT_FOUND,
            "RESOURCE_NOT_FOUND",
            "店铺不存在")
    );
    return ShopView.from(shop);
    }

    public ShopView updateShop(Long merchantId, Long shopId, ShopUpdateRequest request) {
        //先找商家
        Shop shop=shopMapper.findById(shopId).orElseThrow(()->
        new BusinessException(
            HttpStatus.NOT_FOUND,
            "RESOURCE_NOT_FOUND",
            "店铺不存在")
        );

        if(!shop.getMerchantId().equals(merchantId)){
            throw new BusinessException(
                HttpStatus.FORBIDDEN,
                "FORBIDDEN",
            "无权修改该店铺"
            );
        }

        shop.updateInfo(
            request.shopName().trim(),
            request.notice().trim()
        );

        shopMapper.updateInfo(shop);

        return ShopView.from(shop);
    }

    public ShopPage listShops(Integer page, Integer size) {
        return listShops(page, size, null);
    }

    public ShopPage listShops(Integer page, Integer size, String businessScope){
        return listShops(page, size, businessScope, null);
    }

    public ShopPage searchShops(Integer page, Integer size, String keyword) {
        String word = keyword == null ? "" : keyword.trim();
        if (word.isEmpty()) {
            throw new BusinessException(
                HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "搜索关键词不能为空");
        }

        int currentPage = page == null ? 1 : page;
        int pageSize = size == null ? 20 : size;
        if (currentPage < 1) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "页码必须大于等于1");
        }
        if (pageSize < 1 || pageSize > 100) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "每页数量必须在1到100之间");
        }

        int offset = (currentPage - 1) * pageSize;
        List<ShopView> items = shopMapper.searchPage(word, pageSize, offset)
                .stream()
                .map(ShopView::from)
                .toList();
        long total = shopMapper.countByKeyword(word);
        int totalPages = (int) ((total + pageSize - 1) / pageSize);

        return new ShopPage(items, currentPage, pageSize, total, totalPages);
    }

    public ShopPage listShops(Integer page, Integer size, String businessScope, Long businessCategoryId){
        int currentPage=page==null?1:page;
        int pageSize=size==null?20:size;
        if(currentPage<1){
            throw new BusinessException(
                HttpStatus.BAD_REQUEST,
                "VALIDATION_ERROR",
                "页码必须大于等于1"
            );
        }

        if(pageSize<1||pageSize>100){
            throw new BusinessException(
                HttpStatus.BAD_REQUEST,
                "VALIDATION_ERROR",
                "每页数量必须在1到100之间"
            );
        }

        int offset = (currentPage - 1) * pageSize;

        String scope = businessScope == null ? "" : businessScope.trim();

        List<Shop> shopList;
        long total;

        if (businessCategoryId != null) {
            if (businessCategoryId <= 0) throw new BusinessException(HttpStatus.BAD_REQUEST,"VALIDATION_ERROR","经营品类ID不合法");
            shopList = shopMapper.findPageByBusinessCategoryId(businessCategoryId,pageSize,offset);
            total = shopMapper.countByBusinessCategoryId(businessCategoryId);
        } else if (scope.isEmpty()) {
            shopList = shopMapper.findPage(pageSize, offset);
            total = shopMapper.countAll();
        } else {
            shopList = shopMapper.findPageByBusinessScope(
                    scope,
                    pageSize,
                    offset
            );
            total = shopMapper.countByBusinessScope(scope);
        }

        List<ShopView> items = shopList.stream()
                .map(ShopView::from)
                .toList();
        int totalPages = (int) ((total + pageSize - 1) / pageSize);

        return new ShopPage(items, currentPage, pageSize, total, totalPages);
    }
    public ShopView updateBusinessHours(
        Long merchantId,
        Long shopId,
        ShopBusinessHoursRequest request) {

        Shop shop = shopMapper.findById(shopId)
                .orElseThrow(() -> new BusinessException(
                        HttpStatus.NOT_FOUND,
                        "RESOURCE_NOT_FOUND",
                        "店铺不存在"
                ));

        if (!shop.getMerchantId().equals(merchantId)) {
            throw new BusinessException(
                    HttpStatus.FORBIDDEN,
                    "FORBIDDEN",
                    "无权修改该店铺"
            );
        }

        if (request.closingTime().equals(request.openingTime())) {
            throw new BusinessException(
                    HttpStatus.BAD_REQUEST,
                    "VALIDATION_ERROR",
                    "营业开始与结束时间不能相同"
            );
        }

        shop.updateBusinessHours(
                request.openingTime(),
                request.closingTime()
        );

        int updated = shopMapper.updateBusinessHours(
                shopId,
                request.openingTime(),
                request.closingTime()
        );

        if (updated == 0) {
            throw new BusinessException(
                    HttpStatus.NOT_FOUND,
                    "RESOURCE_NOT_FOUND",
                    "店铺不存在"
            );
        }

        return ShopView.from(shop);
    }
}
