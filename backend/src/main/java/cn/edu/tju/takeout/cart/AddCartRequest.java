package cn.edu.tju.takeout.cart;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record AddCartRequest(@NotNull Long productId, @NotNull @Positive Integer quantity) {}
