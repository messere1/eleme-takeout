package cn.edu.tju.takeout.refund;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
public record RefundDecisionRequest(
        @NotBlank @Pattern(regexp = "APPROVED|REJECTED") String status) {}
