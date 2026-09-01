package cn.edu.tju.takeout.shop;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record ShopStatusRequest(
        @NotBlank(message = "营业状态不能为空")
        @Pattern(regexp = "OPEN|CLOSED|TEMP_CLOSED", message = "营业状态不合法") String status) {
}
