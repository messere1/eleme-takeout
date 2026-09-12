package cn.edu.tju.takeout.refund;
import cn.edu.tju.takeout.common.StrictMoneyDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.List;
public record CreateRefundRequest(
        @NotNull
        @DecimalMin("0.01")
        @Digits(integer = 8, fraction = 2)
        @JsonDeserialize(using = StrictMoneyDeserializer.class)
        BigDecimal amount,
        @NotBlank @Size(max = 255) String reason,
        @Size(max = 3) List<@NotBlank @Size(max = 255) String> evidenceUrls) {}
