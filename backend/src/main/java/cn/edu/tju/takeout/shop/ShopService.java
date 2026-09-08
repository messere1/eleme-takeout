package cn.edu.tju.takeout.shop;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import cn.edu.tju.takeout.common.BusinessException;

@Service
public class ShopService {

    private final ShopMapper shopMapper;

    public ShopService(ShopMapper shopMapper) {
        // 仅保留依赖签名，等待功能开发人员实现。
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

    public ShopPage listShops(Integer page, Integer size){
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

        List<ShopView> items = shopMapper.findPage(pageSize, offset)
            .stream()
            .map(ShopView::from)
            .toList();

        long total = shopMapper.countAll();
        int totalPages = (int) ((total + pageSize - 1) / pageSize);

        return new ShopPage(
            items,
            currentPage,
            pageSize,
            total,
            totalPages
        );
    }
    private UnsupportedOperationException pending() {
        return new UnsupportedOperationException("待功能开发：店铺业务尚未实现");
    }
}
