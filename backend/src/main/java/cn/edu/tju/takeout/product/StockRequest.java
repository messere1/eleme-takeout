package cn.edu.tju.takeout.product;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record StockRequest(@NotNull @PositiveOrZero Integer stock) {}
