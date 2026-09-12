package cn.edu.tju.takeout.order;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateOrderRequest(
        @NotNull Long shopId,
        @NotBlank @Size(max = 50) String recipientName,
        @NotBlank @Pattern(regexp = "\\+?[0-9 -]{7,20}") String recipientPhone,
        @NotBlank @Size(min = 5, max = 255) String deliveryAddress,
        Boolean saveToProfile) {
    public CreateOrderRequest(String address) {
        this(null, "收货人", "0000000", address == null ? "未填写地址" : address, false);
    }
}
