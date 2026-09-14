package cn.edu.tju.takeout.admin;

import cn.edu.tju.takeout.auth.UserPrincipal;
import cn.edu.tju.takeout.common.ApiResponse;
import cn.edu.tju.takeout.common.BusinessException;
import cn.edu.tju.takeout.merchant.Merchant;
import cn.edu.tju.takeout.merchant.MerchantMapper;
import cn.edu.tju.takeout.order.Order;
import cn.edu.tju.takeout.order.OrderMapper;
import cn.edu.tju.takeout.product.ProductMapper;
import cn.edu.tju.takeout.user.User;
import cn.edu.tju.takeout.user.UserMapper;
import jakarta.validation.Valid;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin")
public class AdminController {
    private static final Set<String> PRODUCT_STATUSES = Set.of("ON_SALE", "OFF_SALE");
    private static final Set<String> ORDER_STATUSES = Set.of(
            "CREATED", "ACCEPTED", "DELIVERING", "DELIVERED", "COMPLETED", "CANCELLED");

    private final UserMapper users;
    private final MerchantMapper merchants;
    private final ProductMapper products;
    private final OrderMapper orders;
    private final AdminMapper admins;

    public AdminController(
            UserMapper users,
            MerchantMapper merchants,
            ProductMapper products,
            OrderMapper orders,
            AdminMapper admins) {
        this.users = users;
        this.merchants = merchants;
        this.products = products;
        this.orders = orders;
        this.admins = admins;
    }

    @GetMapping("/users")
    public ApiResponse<Map<String, Object>> users(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        int offset = offset(page, size);
        List<Map<String, Object>> items = users.findPage(size, offset).stream()
                .map(this::userView)
                .toList();
        return ApiResponse.success(page(items, page, size, users.countAll()));
    }

    @GetMapping("/merchants")
    public ApiResponse<Map<String, Object>> merchants(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        int offset = offset(page, size);
        List<Map<String, Object>> items = merchants.findPage(size, offset).stream()
                .map(this::merchantView)
                .toList();
        return ApiResponse.success(page(items, page, size, merchants.countAll()));
    }

    @GetMapping("/products")
    public ApiResponse<Map<String, Object>> products(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        int offset = offset(page, size);
        return ApiResponse.success(page(
                products.findPageForAdmin(size, offset),
                page,
                size,
                products.countForAdmin()));
    }

    @GetMapping("/orders")
    public ApiResponse<Map<String, Object>> orders(
            @RequestParam(required = false) String status,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        int offset = offset(page, size);
        String normalizedStatus = normalizeOrderStatus(status);
        validateTimeRange(startTime, endTime);
        List<AdminOrderView> items = orders
                .findPageForAdmin(normalizedStatus, startTime, endTime, size, offset)
                .stream()
                .map(this::orderView)
                .toList();
        return ApiResponse.success(page(
                items,
                page,
                size,
                orders.countForAdmin(normalizedStatus, startTime, endTime)));
    }

    @PatchMapping("/users/{id}/status")
    @Transactional
    public ApiResponse<Void> userStatus(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id,
            @Valid @RequestBody AdminStatusRequest request) {
        boolean enabled = parseEnabled(request.status());
        if (users.setEnabled(id, enabled) == 0) {
            throw notFound();
        }
        admins.audit(principal.userId(), "SET_STATUS", "USER", id);
        return ApiResponse.success(null);
    }

    @PatchMapping("/merchants/{id}/status")
    @Transactional
    public ApiResponse<Void> merchantStatus(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id,
            @Valid @RequestBody AdminStatusRequest request) {
        boolean enabled = parseEnabled(request.status());
        if (merchants.setEnabled(id, enabled) == 0) {
            throw notFound();
        }
        admins.audit(principal.userId(), "SET_STATUS", "MERCHANT", id);
        return ApiResponse.success(null);
    }

    @PatchMapping("/products/{id}/status")
    @Transactional
    public ApiResponse<Void> productStatus(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id,
            @Valid @RequestBody AdminStatusRequest request) {
        validateStatus(request.status(), PRODUCT_STATUSES);
        if (products.setStatusForAdmin(id, request.status()) == 0) {
            throw notFound();
        }
        admins.audit(principal.userId(), "SET_STATUS", "PRODUCT", id);
        return ApiResponse.success(null);
    }

    @PatchMapping("/orders/{id}/status")
    @Transactional
    public ApiResponse<Void> orderStatus(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long id,
            @Valid @RequestBody AdminStatusRequest request) {
        validateStatus(request.status(), ORDER_STATUSES);
        if (orders.setStatusForAdmin(id, request.status()) == 0) {
            throw notFound();
        }
        admins.audit(principal.userId(), "SET_STATUS", "ORDER", id);
        return ApiResponse.success(null);
    }

    private Map<String, Object> userView(User user) {
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("id", user.getId());
        view.put("username", user.getUsername());
        view.put("phone", maskPhone(user.getPhone()));
        view.put("nickname", user.getNickname());
        view.put("enabled", user.isEnabled());
        view.put("createdAt", user.getCreatedAt());
        return view;
    }

    private Map<String, Object> merchantView(Merchant merchant) {
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("id", merchant.getId());
        view.put("merchantName", merchant.getMerchantName());
        view.put("phone", maskPhone(merchant.getPhone()));
        view.put("businessScope", merchant.getBusinessScope());
        view.put("enabled", merchant.isEnabled());
        view.put("createdAt", merchant.getCreatedAt());
        return view;
    }

    private AdminOrderView orderView(Order order) {
        return AdminOrderView.from(order, maskPhone(order.getRecipientPhone()));
    }

    private int offset(int page, int size) {
        if (page < 1 || size < 1 || size > 100) {
            throw validationError("分页参数不合法");
        }
        long offset = ((long) page - 1) * size;
        if (offset > Integer.MAX_VALUE) {
            throw validationError("分页参数不合法");
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

    private boolean parseEnabled(String status) {
        return switch (status) {
            case "ENABLED" -> true;
            case "DISABLED" -> false;
            default -> throw validationError("状态不合法");
        };
    }

    private String normalizeOrderStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        validateStatus(status, ORDER_STATUSES);
        return status;
    }

    private void validateStatus(String status, Set<String> allowedStatuses) {
        if (!allowedStatuses.contains(status)) {
            throw validationError("资源状态不合法");
        }
    }

    private void validateTimeRange(LocalDateTime startTime, LocalDateTime endTime) {
        if (startTime != null && endTime != null && startTime.isAfter(endTime)) {
            throw validationError("开始时间不能晚于结束时间");
        }
    }

    private String maskPhone(String phone) {
        if (phone == null) {
            return "";
        }
        String normalized = phone.trim();
        if (normalized.length() < 7) {
            return "***";
        }
        return normalized.substring(0, 3)
                + "****"
                + normalized.substring(normalized.length() - 4);
    }

    private BusinessException validationError(String message) {
        return new BusinessException(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", message);
    }

    private BusinessException notFound() {
        return new BusinessException(
                HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND", "资源不存在");
    }
}
