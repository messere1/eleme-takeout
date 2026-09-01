package cn.edu.tju.takeout.product;

import jakarta.validation.constraints.Pattern;

public record ProductStatusRequest(
        @Pattern(regexp = "ON_SALE|OFF_SALE") String status) {}
