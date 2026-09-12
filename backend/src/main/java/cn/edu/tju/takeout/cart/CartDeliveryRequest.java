package cn.edu.tju.takeout.cart;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CartDeliveryRequest(
        @NotNull Long shopId,
        @NotBlank @Size(max = 50) String recipientName,
        @NotBlank @Pattern(regexp = "\\+?[0-9 -]{7,20}") String recipientPhone,
        @NotBlank @Size(min = 5, max = 255) String deliveryAddress,
        Boolean saveToProfile) {}
