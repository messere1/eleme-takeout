package cn.edu.tju.takeout.refund;
import cn.edu.tju.takeout.auth.UserPrincipal;import cn.edu.tju.takeout.common.*;import cn.edu.tju.takeout.order.*;
import jakarta.validation.Valid;import java.math.BigDecimal;import java.util.List;import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/v1") public class RefundController {
 private final RefundMapper refunds;private final OrderMapper orders;
 public RefundController(RefundMapper refunds,OrderMapper orders){this.refunds=refunds;this.orders=orders;}
 @PostMapping("/orders/{orderId}/refunds") public ApiResponse<RefundRequest> create(@AuthenticationPrincipal UserPrincipal p,@PathVariable Long orderId,@Valid @RequestBody CreateRefundRequest body){
  Order o=orders.findById(orderId).orElseThrow(()->err(HttpStatus.NOT_FOUND,"RESOURCE_NOT_FOUND","订单不存在"));
  if(!o.getUserId().equals(p.userId()))throw err(HttpStatus.FORBIDDEN,"FORBIDDEN","无权申请该订单退款");
  if(!"PAID".equals(o.getPaymentStatus()))throw err(HttpStatus.CONFLICT,"BUSINESS_CONFLICT","仅已支付订单可退款");
  BigDecimal left=o.getTotalAmount().subtract(refunds.reserved(orderId));if(body.amount().compareTo(left)>0)throw err(HttpStatus.CONFLICT,"BUSINESS_CONFLICT","退款金额超过可退金额");
  RefundRequest r=RefundRequest.pending(orderId,p.userId(),body.amount(),body.reason().trim(),body.evidenceUrls()==null?null:String.join(",",body.evidenceUrls()));refunds.insert(r);return ApiResponse.success(r);}
 @GetMapping("/refunds") public ApiResponse<List<RefundRequest>> mine(@AuthenticationPrincipal UserPrincipal p){return ApiResponse.success(refunds.findByUser(p.userId()));}
 @GetMapping("/admin/refunds") public ApiResponse<List<RefundRequest>> all(){return ApiResponse.success(refunds.findAll());}
 @PatchMapping("/admin/refunds/{id}") public ApiResponse<RefundRequest> decide(@PathVariable Long id,@Valid @RequestBody RefundDecisionRequest b){if(refunds.decide(id,b.status())==0)throw err(HttpStatus.CONFLICT,"BUSINESS_CONFLICT","退款已处理或不存在");return ApiResponse.success(refunds.findById(id).orElseThrow());}
 @GetMapping("/merchant/refunds") public ApiResponse<List<RefundRequest>> merchant(@AuthenticationPrincipal UserPrincipal p){return ApiResponse.success(refunds.findByMerchant(p.userId()));}
 @PatchMapping("/merchant/refunds/{id}") public ApiResponse<RefundRequest> merchantDecide(@AuthenticationPrincipal UserPrincipal p,@PathVariable Long id,@Valid @RequestBody RefundDecisionRequest b){if(refunds.decideForMerchant(id,p.userId(),b.status())==0)throw err(HttpStatus.CONFLICT,"BUSINESS_CONFLICT","退款已处理、不存在或不属于本店");return ApiResponse.success(refunds.findById(id).orElseThrow());}
 private BusinessException err(HttpStatus s,String c,String m){return new BusinessException(s,c,m);}
}
