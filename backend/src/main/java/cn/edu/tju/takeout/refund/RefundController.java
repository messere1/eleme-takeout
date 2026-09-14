package cn.edu.tju.takeout.refund;

import cn.edu.tju.takeout.admin.AdminMapper;
import cn.edu.tju.takeout.auth.UserPrincipal;
import cn.edu.tju.takeout.common.ApiResponse;
import cn.edu.tju.takeout.common.BusinessException;
import cn.edu.tju.takeout.order.Order;
import cn.edu.tju.takeout.order.OrderMapper;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class RefundController {
    private static final Set<String> DECISION_STATUSES = Set.of("APPROVED", "REJECTED");

    private final RefundMapper refunds;
    private final OrderMapper orders;
    private final AdminMapper admins;

    @Autowired
    public RefundController(RefundMapper refunds, OrderMapper orders, AdminMapper admins) {
        this.refunds = refunds;
        this.orders = orders;
        this.admins = admins;
    }

    @PostMapping("/orders/{orderId}/refunds")
    @Transactional
    public ApiResponse<RefundRequest> create(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long orderId,
            @Valid @RequestBody CreateRefundRequest body) {
        Order order = orders.findByIdForUpdate(orderId)
                .orElseThrow(() -> notFound("订单不存在"));
        if (!order.getUserId().equals(principal.userId())) {
            throw forbidden("无权申请该订单退款");
        }
        if (!"PAID".equals(order.getPaymentStatus())) {
            throw conflict("仅已支付订单可退款");
        }
        if (body.amount().compareTo(BigDecimal.ZERO) <= 0) {
            throw validation("退款金额必须大于0");
        }

        BigDecimal remaining = order.getTotalAmount().subtract(refunds.reserved(orderId));
        if (remaining.compareTo(BigDecimal.ZERO) <= 0
                || body.amount().compareTo(remaining) > 0) {
            throw conflict("退款金额超过可退金额");
        }

        String reason = body.reason().trim();
        if (reason.isEmpty()) {
            throw validation("退款原因不能为空");
        }
        List<String> evidence = body.evidenceUrls();
        if (evidence != null && evidence.stream().anyMatch(url -> url == null || url.isBlank())) {
            throw validation("退款证据地址不能为空");
        }

        RefundRequest refund = RefundRequest.pending(
                orderId,
                principal.userId(),
                body.amount(),
                reason,
                evidence == null || evidence.isEmpty() ? null : String.join(",", evidence));
        refunds.insert(refund);
        return ApiResponse.success(refund);
    }

    @GetMapping("/refunds")
    public ApiResponse<List<RefundRequest>> mine(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.success(refunds.findByUser(principal.userId()));
    }

    // FR-016：管理员退款列表同样按分页查询，与其他 admin 列表保持一致。
    @GetMapping("/admin/refunds")
    public ApiResponse<Map<String, Object>> all(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        int offset = offset(page, size);
        return ApiResponse.success(page(
                refunds.findPageForAdmin(size, offset),
                page,
                size,
                refunds.countAll()));
    }

    @PatchMapping("/admin/refunds/{id}")
    @Transactional
    public ApiResponse<RefundRequest> decide(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id,
            @Valid @RequestBody RefundDecisionRequest body) {
        validateDecision(body.status());
        RefundRequest current = refunds.findById(id)
                .orElseThrow(() -> notFound("退款申请不存在"));
        if (!"PENDING".equals(current.getStatus())) {
            throw conflict("退款已处理");
        }
        if (refunds.decide(id, body.status()) == 0) {
            throw conflict("退款已处理");
        }
        // SRS §7：管理员写操作必须留痕（该端点由 SecurityConfig 限制为 ADMIN，principal 非空）。
        admins.audit(principal.userId(), "SET_STATUS", "REFUND", id);
        return ApiResponse.success(refunds.findById(id)
                .orElseThrow(() -> notFound("退款申请不存在")));
    }

    @GetMapping("/merchant/refunds")
    public ApiResponse<List<RefundRequest>> merchant(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.success(refunds.findByMerchant(principal.userId()));
    }

    @PatchMapping("/merchant/refunds/{id}")
    @Transactional
    public ApiResponse<RefundRequest> merchantDecide(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id,
            @Valid @RequestBody RefundDecisionRequest body) {
        validateDecision(body.status());
        RefundRequest current = refunds.findById(id)
                .orElseThrow(() -> notFound("退款申请不存在"));
        if (!"PENDING".equals(current.getStatus())) {
            throw conflict("退款已处理");
        }
        if (refunds.decideForMerchant(id, principal.userId(), body.status()) == 0) {
            throw forbidden("退款申请不属于本店");
        }
        return ApiResponse.success(refunds.findById(id)
                .orElseThrow(() -> notFound("退款申请不存在")));
    }

    private void validateDecision(String status) {
        // DECISION_STATUSES 由 Set.of 构造，是不可变集合：contains(null) 会抛 NPE 而不是返回 false。
        if (status == null || !DECISION_STATUSES.contains(status)) {
            throw validation("退款处理状态不合法");
        }
    }

    private int offset(int page, int size) {
        if (page < 1 || size < 1 || size > 100) {
            throw validation("分页参数不合法");
        }
        long offset = ((long) page - 1) * size;
        if (offset > Integer.MAX_VALUE) {
            throw validation("分页参数不合法");
        }
        return (int) offset;
    }

    private Map<String, Object> page(Object items, int page, int size, long total) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("items", items);
        result.put("page", page);
        result.put("size", size);
        result.put("total", total);
        result.put("totalPages", total == 0 ? 0 : (int) ((total + size - 1) / size));
        return result;
    }

    private BusinessException validation(String message) {
        return new BusinessException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", message);
    }

    private BusinessException forbidden(String message) {
        return new BusinessException(HttpStatus.FORBIDDEN, "FORBIDDEN", message);
    }

    private BusinessException notFound(String message) {
        return new BusinessException(HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND", message);
    }

    private BusinessException conflict(String message) {
        return new BusinessException(HttpStatus.CONFLICT, "BUSINESS_CONFLICT", message);
    }
}
