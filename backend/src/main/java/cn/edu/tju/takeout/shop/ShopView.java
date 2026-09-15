package cn.edu.tju.takeout.shop;
import java.time.LocalTime;

public record ShopView(
    Long id, 
    Long merchantId, 
    String shopName, 
    String notice, 
    String status,
    String shopAddress, 
    String imageUrl, 
    String coverImageUrl,
    LocalTime openingTime,
    LocalTime closingTime
) {
    public ShopView(
        Long id, 
        Long merchantId, 
        String shopName, 
        String notice, 
        String status
    ) {
        this(
            id, 
            merchantId, 
            shopName, 
            notice, 
            status, 
            null, 
            null, 
            null,
            null,
            null);
    }
    public static ShopView from(Shop shop) {
        return new ShopView(
            shop.getId(), 
            shop.getMerchantId(), 
            shop.getShopName(), 
            shop.getNotice(), 
            shop.getStatus(),
            shop.getShopAddress(), 
            shop.getImageUrl(), 
            shop.getCoverImageUrl(),
            shop.getOpeningTime(),
            shop.getClosingTime());
    }
}
