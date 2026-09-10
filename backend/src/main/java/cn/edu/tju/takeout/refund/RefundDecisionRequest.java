package cn.edu.tju.takeout.refund;
import jakarta.validation.constraints.Pattern;
public record RefundDecisionRequest(@Pattern(regexp="APPROVED|REJECTED") String status) {}
