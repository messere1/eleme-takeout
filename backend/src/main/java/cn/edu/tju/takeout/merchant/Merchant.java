package cn.edu.tju.takeout.merchant;

import java.time.LocalDateTime;

public class Merchant {
    private Long id;
    private String merchantName;
    private String phone;
    private String passwordHash;
    private String businessScope;
    private LocalDateTime createdAt;

    public static Merchant registered(String name, String phone, String passwordHash, String scope) {
        Merchant merchant = new Merchant();
        merchant.merchantName = name;
        merchant.phone = phone;
        merchant.passwordHash = passwordHash;
        merchant.businessScope = scope;
        merchant.createdAt = LocalDateTime.now();
        return merchant;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getMerchantName() { return merchantName; }
    public String getPhone() { return phone; }
    public String getPasswordHash() { return passwordHash; }
    public String getBusinessScope() { return businessScope; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
