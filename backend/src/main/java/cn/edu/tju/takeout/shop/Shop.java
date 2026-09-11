package cn.edu.tju.takeout.shop;

public class Shop {
    private Long id;
    private Long merchantId;
    private String shopName;
    private String notice;
    private String status;
    private String shopAddress;
    private String imageUrl;
    private String coverImageUrl;

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
    public String getShopAddress() { return shopAddress; }
    public String getImageUrl() { return imageUrl; }
    public String getCoverImageUrl() { return coverImageUrl; }
    public void setShopAddress(String shopAddress) { this.shopAddress = shopAddress; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public void setCoverImageUrl(String coverImageUrl) { this.coverImageUrl = coverImageUrl; }
    public void changeStatus(String status) { this.status = status; }
    public void updateInfo(String shopName, String notice) {
        this.shopName = shopName;
        this.notice = notice;
    }
}
