package cn.edu.tju.takeout.refund;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.List;
public record CreateRefundRequest(@NotNull @DecimalMin("0.01") @Digits(integer=8,fraction=2) BigDecimal amount,
        @NotBlank @Size(max=255) String reason, @Size(max=3) List<@Size(max=255) String> evidenceUrls) {}
