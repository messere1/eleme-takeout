package cn.edu.tju.takeout.product;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record ProductStatusRequest(
        @NotBlank(message = "商品状态不能为空")
        @Pattern(regexp = "ON_SALE|OFF_SALE", message = "商品状态不合法")
        String status) {}
