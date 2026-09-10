package cn.edu.tju.takeout.merchant;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record MerchantRegistrationRequest(
        @NotBlank(message = "商家名称不能为空") @Size(max = 50, message = "商家名称不能超过50个字符") String merchantName,
        @NotBlank(message = "手机号不能为空") @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确") String phone,
        @NotBlank(message = "密码不能为空") @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).{6,64}$", message = "密码必须为6到64位且同时包含字母和数字") String password,
        @NotBlank(message = "经营范围不能为空") @Size(max = 50, message = "经营范围不能超过50个字符") String businessScope,
        @Size(min = 5, max = 255, message = "店铺地址长度应为5到255个字符") String shopAddress) {

    public MerchantRegistrationRequest(String merchantName, String phone, String password, String businessScope) {
        this(merchantName, phone, password, businessScope, "地址待完善");
    }

    @Override
    public String toString() {
        return "MerchantRegistrationRequest[merchantName=" + merchantName
                + ", phone=" + mask(phone)
                + ", password=***"
                + ", businessScope=" + businessScope + "]";
    }

    private static String mask(String value) {
        if (value == null || value.length() < 7) {
            return "***";
        }
        return value.substring(0, 3) + "****" + value.substring(value.length() - 4);
    }
}
