package cn.edu.tju.takeout.merchant;
import jakarta.validation.constraints.*;
public record MerchantProfileUpdateRequest(@NotBlank @Size(max=50) String businessScope,
 @NotBlank @Size(max=50) String shopName,@NotBlank @Size(min=5,max=255) String shopAddress){}
