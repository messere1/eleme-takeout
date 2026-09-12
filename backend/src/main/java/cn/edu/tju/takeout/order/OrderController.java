package cn.edu.tju.takeout.order;

import cn.edu.tju.takeout.auth.UserPrincipal;
import cn.edu.tju.takeout.common.ApiResponse;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import java.time.LocalDateTime;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {
    private final OrderService orderService;

    public OrderController(OrderService orderService) { this.orderService = orderService; }

    @PostMapping
    public ApiResponse<OrderView> create(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody CreateOrderRequest request) {
        if (request.shopId() == null && "收货人".equals(request.recipientName())) {
            return ApiResponse.success(orderService.create(principal.userId(), request.deliveryAddress()));
        }
        return ApiResponse.success(orderService.create(principal.userId(), request));
    }

    @GetMapping
    public ApiResponse<OrderPage> list(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) String status,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "20") Integer size) {
        return ApiResponse.success(orderService.list(
                principal.userId(), new OrderQuery(status, startTime, endTime, page, size)));
    }

    @GetMapping("/{orderId}")
    public ApiResponse<OrderView> getDetail(
            @AuthenticationPrincipal UserPrincipal principal, @PathVariable Long orderId) {
        return ApiResponse.success(
                orderService.getDetail(principal.userId(), principal.role(), orderId));
    }

    @PostMapping("/{orderId}/cancel")
    public ApiResponse<OrderView> cancel(
            @AuthenticationPrincipal UserPrincipal principal, @PathVariable Long orderId) {
        return ApiResponse.success(orderService.cancel(principal.userId(), orderId));
    }

    @PostMapping("/{orderId}/accept")
    public ApiResponse<OrderView> accept(
            @AuthenticationPrincipal UserPrincipal principal, @PathVariable Long orderId) {
        return ApiResponse.success(orderService.accept(principal.userId(), orderId));
    }

    @PostMapping("/{orderId}/confirm")
    public ApiResponse<OrderView> confirm(
            @AuthenticationPrincipal UserPrincipal principal, @PathVariable Long orderId) {
        return ApiResponse.success(orderService.confirmReceived(principal.userId(), orderId));
    }

    @PostMapping("/{orderId}/pay")
    public ApiResponse<OrderView> pay(
            @AuthenticationPrincipal UserPrincipal principal, @PathVariable Long orderId) {
        return ApiResponse.success(orderService.pay(principal.userId(), orderId));
    }
}
