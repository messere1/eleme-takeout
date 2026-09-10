package cn.edu.tju.takeout.merchant;

public record MerchantProfileView(
        Long id, String merchantName, String phone, String businessScope,
        Long shopId, String shopName, String shopStatus) {}
