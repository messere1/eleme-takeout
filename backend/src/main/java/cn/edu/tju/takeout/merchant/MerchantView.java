package cn.edu.tju.takeout.merchant;

public record MerchantView(Long id, String merchantName, String phone, String businessScope,
                           Long shopId, String shopStatus) {
}
