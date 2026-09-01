package cn.edu.tju.takeout.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UserProfileUpdateRequest(
        @NotBlank(message = "昵称不能为空")
        @Size(max = 30, message = "昵称不能超过30个字符") String nickname,
        @NotBlank(message = "手机号不能为空")
        @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确") String phone,
        @NotBlank(message = "收货地址不能为空")
        @Size(max = 255, message = "收货地址不能超过255个字符") String address) {
}
