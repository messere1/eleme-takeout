package cn.edu.tju.takeout.shop;

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

    private UnsupportedOperationException pending() {
        return new UnsupportedOperationException("待功能开发：店铺业务尚未实现");
    }
}
