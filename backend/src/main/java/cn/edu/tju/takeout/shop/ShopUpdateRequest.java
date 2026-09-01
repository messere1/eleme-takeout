package cn.edu.tju.takeout.shop;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ShopUpdateRequest(
        @NotBlank(message = "店铺名称不能为空") @Size(max = 50, message = "店铺名称不能超过50个字符") String shopName,
        @NotBlank(message = "店铺公告不能为空") @Size(max = 255, message = "店铺公告不能超过255个字符") String notice) {
}
