package cn.edu.tju.takeout.merchant;

public record MerchantProfileView(
        Long id, String merchantName, String phone, String businessScope,
        Long shopId, String shopName, String shopStatus, String shopAddress, String imageUrl, String coverImageUrl) {
    public MerchantProfileView(Long id,String merchantName,String phone,String businessScope,Long shopId,String shopName,String shopStatus){
        this(id,merchantName,phone,businessScope,shopId,shopName,shopStatus,null,null,null);
    }
}
