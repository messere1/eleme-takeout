package cn.edu.tju.takeout.shop;

import jakarta.validation.constraints.NotNull;
import java.time.LocalTime;

public record ShopBusinessHoursRequest(
        @NotNull(message = "开始营业时间不能为空")
        LocalTime openingTime,

        @NotNull(message = "结束营业时间不能为空")
        LocalTime closingTime
) {
}
