package cn.edu.tju.takeout.rider;

import cn.edu.tju.takeout.auth.UserPrincipal;
import cn.edu.tju.takeout.common.*;
import cn.edu.tju.takeout.order.*;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/rider/orders")
public class RiderController {
    private final OrderMapper orders;
    public RiderController(OrderMapper orders) { this.orders = orders; }
    @GetMapping("/available") public ApiResponse<List<Order>> available() { return ApiResponse.success(orders.findReadyForDelivery()); }
    @GetMapping public ApiResponse<List<Order>> mine(@AuthenticationPrincipal UserPrincipal p) { return ApiResponse.success(orders.findByRiderId(p.userId())); }
    @PostMapping("/{id}/claim") public ApiResponse<Order> claim(@AuthenticationPrincipal UserPrincipal p,@PathVariable Long id) {
        if(orders.claimForDelivery(id,p.userId())==0) conflict("订单已被领取或状态不可配送"); return ApiResponse.success(orders.findById(id).orElseThrow()); }
    @PostMapping("/{id}/deliver") public ApiResponse<Order> deliver(@AuthenticationPrincipal UserPrincipal p,@PathVariable Long id) {
        if(orders.markDelivered(id,p.userId())==0) conflict("订单不属于当前骑手或状态不可送达"); return ApiResponse.success(orders.findById(id).orElseThrow()); }
    private void conflict(String m){throw new BusinessException(HttpStatus.CONFLICT,"BUSINESS_CONFLICT",m);}
}
