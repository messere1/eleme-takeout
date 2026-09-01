package cn.edu.tju.takeout.shop;

public record ShopView(Long id, Long merchantId, String shopName, String notice, String status) {
    static ShopView from(Shop shop) {
        return new ShopView(shop.getId(), shop.getMerchantId(), shop.getShopName(), shop.getNotice(), shop.getStatus());
    }
}
