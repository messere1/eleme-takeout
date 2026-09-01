package cn.edu.tju.takeout.shop;

public class Shop {
    private Long id;
    private Long merchantId;
    private String shopName;
    private String notice;
    private String status;

    public static Shop initiallyClosed(Long merchantId, String name) {
        Shop shop = new Shop();
        shop.merchantId = merchantId;
        shop.shopName = name;
        shop.status = "CLOSED";
        return shop;
    }
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getMerchantId() { return merchantId; }
    public String getShopName() { return shopName; }
    public String getNotice() { return notice; }
    public String getStatus() { return status; }
    public void changeStatus(String status) { this.status = status; }
    public void updateInfo(String shopName, String notice) {
        this.shopName = shopName;
        this.notice = notice;
    }
}
