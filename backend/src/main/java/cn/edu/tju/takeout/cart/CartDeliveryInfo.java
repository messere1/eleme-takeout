package cn.edu.tju.takeout.cart;

public class CartDeliveryInfo {
    private Long userId;
    private Long shopId;
    private String recipientName;
    private String recipientPhone;
    private String deliveryAddress;

    public static CartDeliveryInfo of(Long userId, Long shopId, String name, String phone, String address) {
        CartDeliveryInfo info = new CartDeliveryInfo();
        info.userId = userId; info.shopId = shopId; info.recipientName = name;
        info.recipientPhone = phone; info.deliveryAddress = address;
        return info;
    }
    public Long getUserId() { return userId; }
    public Long getShopId() { return shopId; }
    public String getRecipientName() { return recipientName; }
    public String getRecipientPhone() { return recipientPhone; }
    public String getDeliveryAddress() { return deliveryAddress; }
}
