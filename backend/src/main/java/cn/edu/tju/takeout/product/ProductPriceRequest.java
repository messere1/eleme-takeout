package cn.edu.tju.takeout.product;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record ProductPriceRequest(
        @NotNull(message = "商品价格不能为空")
        @DecimalMin(value = "0.00", inclusive = false, message = "商品价格必须大于0")
        @Digits(integer = 8, fraction = 2, message = "商品价格整数最多8位，小数最多2位")
        BigDecimal price) {}
