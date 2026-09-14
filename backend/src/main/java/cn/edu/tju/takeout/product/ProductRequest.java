package cn.edu.tju.takeout.product;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import cn.edu.tju.takeout.common.StrictMoneyDeserializer;

public record ProductRequest(
        @NotBlank @Size(max = 50) String name,
        @NotNull Long categoryId,
        @Size(max = 500) String description,
        @NotNull @DecimalMin(value = "0.00", inclusive = false) @Digits(integer = 8, fraction = 2)
        @JsonDeserialize(using = StrictMoneyDeserializer.class) BigDecimal price,
        @NotNull @PositiveOrZero Integer stock) {}
