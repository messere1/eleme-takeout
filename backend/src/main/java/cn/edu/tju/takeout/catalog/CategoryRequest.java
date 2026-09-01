package cn.edu.tju.takeout.catalog;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record CategoryRequest(
        @NotBlank @Size(max = 30) String name,
        @NotNull @PositiveOrZero Integer sort) {}
