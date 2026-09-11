package cn.edu.tju.takeout.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record LoginRequest(
        @NotBlank(message = "账号不能为空") String account,
        @NotBlank(message = "密码不能为空") String password,
        @Pattern(regexp = "CUSTOMER|MERCHANT|ADMIN|RIDER", message = "角色不合法") String role) {

    @Override
    public String toString() {
        return "LoginRequest[account=***, password=***, role=" + role + "]";
    }
}
