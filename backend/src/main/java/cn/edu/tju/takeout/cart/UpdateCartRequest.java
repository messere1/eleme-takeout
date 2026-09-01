package cn.edu.tju.takeout.cart;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record UpdateCartRequest(@NotNull @PositiveOrZero Integer quantity) {}
