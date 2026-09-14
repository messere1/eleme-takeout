package cn.edu.tju.takeout.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record AdminStatusRequest(
        @NotBlank
        @Pattern(
                regexp = "ENABLED|DISABLED|ON_SALE|OFF_SALE|"
                        + "CREATED|ACCEPTED|DELIVERING|DELIVERED|COMPLETED|CANCELLED")
        String status) {}
