package cn.edu.tju.takeout.shop;

import org.springframework.stereotype.Service;

@Service
public class ShopService {
    public ShopService(ShopMapper shopMapper) {
        // 仅保留依赖签名，等待功能开发人员实现。
    }

    public ShopView changeStatus(Long merchantId, Long shopId, ShopStatusRequest request) {
        throw pending();
    }

    public ShopView getShop(Long shopId) {
        throw pending();
    }

    public ShopView updateShop(Long merchantId, Long shopId, ShopUpdateRequest request) {
        throw pending();
    }

    private UnsupportedOperationException pending() {
        return new UnsupportedOperationException("待功能开发：店铺业务尚未实现");
    }
}
