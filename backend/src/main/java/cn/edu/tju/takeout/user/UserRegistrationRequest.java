package cn.edu.tju.takeout.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UserRegistrationRequest(
        @NotBlank(message = "用户名不能为空")
        @Size(min = 3, max = 30, message = "用户名长度必须为3到30个字符")
        String username,
        @NotBlank(message = "手机号不能为空")
        @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
        String phone,
        @NotBlank(message = "密码不能为空")
        @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).{6,64}$", message = "密码必须为6到64位且同时包含字母和数字")
        String password) {

    @Override
    public String toString() {
        return "UserRegistrationRequest[username=" + username
                + ", phone=***, password=***]";
    }
}
