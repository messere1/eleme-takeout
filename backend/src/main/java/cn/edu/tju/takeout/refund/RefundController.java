package cn.edu.tju.takeout.refund;
import cn.edu.tju.takeout.auth.UserPrincipal;import cn.edu.tju.takeout.common.*;import cn.edu.tju.takeout.order.*;
import jakarta.validation.Valid;import java.math.BigDecimal;import java.util.List;import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;
@RestController @RequestMapping("/api/v1") public class RefundController {
 private final RefundMapper refunds;private final OrderMapper orders;
 public RefundController(RefundMapper refunds,OrderMapper orders){this.refunds=refunds;this.orders=orders;}
 @PostMapping("/orders/{orderId}/refunds") @Transactional
 public ApiResponse<RefundRequest> create(@AuthenticationPrincipal UserPrincipal p,@PathVariable Long orderId,@Valid @RequestBody CreateRefundRequest body){
  Order o=orders.findByIdForUpdate(orderId).orElseThrow(()->err(HttpStatus.NOT_FOUND,"RESOURCE_NOT_FOUND","订单不存在"));
  if(!o.getUserId().equals(p.userId()))throw err(HttpStatus.FORBIDDEN,"FORBIDDEN","无权申请该订单退款");
  if(!"PAID".equals(o.getPaymentStatus()))throw err(HttpStatus.CONFLICT,"BUSINESS_CONFLICT","仅已支付订单可退款");
  if(body.amount().compareTo(BigDecimal.ZERO)<=0)throw err(HttpStatus.BAD_REQUEST,"VALIDATION_ERROR","退款金额必须大于0");
  BigDecimal left=o.getTotalAmount().subtract(refunds.reserved(orderId));
  if(left.compareTo(BigDecimal.ZERO)<=0||body.amount().compareTo(left)>0)throw err(HttpStatus.CONFLICT,"BUSINESS_CONFLICT","退款金额超过可退金额");
  String reason=body.reason().trim();
  if(reason.isEmpty())throw err(HttpStatus.BAD_REQUEST,"VALIDATION_ERROR","退款原因不能为空");
  List<String> evidence=body.evidenceUrls();
  if(evidence!=null&&evidence.stream().anyMatch(url->url==null||url.isBlank()))throw err(HttpStatus.BAD_REQUEST,"VALIDATION_ERROR","退款证据地址不能为空");
  RefundRequest r=RefundRequest.pending(orderId,p.userId(),body.amount(),reason,
          evidence==null||evidence.isEmpty()?null:String.join(",",evidence));
  refunds.insert(r);
  return ApiResponse.success(r);
 }
 @GetMapping("/refunds") public ApiResponse<List<RefundRequest>> mine(@AuthenticationPrincipal UserPrincipal p){return ApiResponse.success(refunds.findByUser(p.userId()));}
 @GetMapping("/admin/refunds") public ApiResponse<List<RefundRequest>> all(){return ApiResponse.success(refunds.findAll());}
 @PatchMapping("/admin/refunds/{id}") @Transactional
 public ApiResponse<RefundRequest> decide(@PathVariable Long id,@Valid @RequestBody RefundDecisionRequest b){
  RefundRequest current=refunds.findById(id).orElseThrow(()->err(HttpStatus.NOT_FOUND,"RESOURCE_NOT_FOUND","退款申请不存在"));
  if(!"PENDING".equals(current.getStatus()))throw err(HttpStatus.CONFLICT,"BUSINESS_CONFLICT","退款已处理");
  if(refunds.decide(id,b.status())==0)throw err(HttpStatus.CONFLICT,"BUSINESS_CONFLICT","退款已处理");
  return ApiResponse.success(refunds.findById(id).orElseThrow(()->err(HttpStatus.NOT_FOUND,"RESOURCE_NOT_FOUND","退款申请不存在")));
 }
 @GetMapping("/merchant/refunds") public ApiResponse<List<RefundRequest>> merchant(@AuthenticationPrincipal UserPrincipal p){return ApiResponse.success(refunds.findByMerchant(p.userId()));}
 @PatchMapping("/merchant/refunds/{id}") @Transactional
 public ApiResponse<RefundRequest> merchantDecide(@AuthenticationPrincipal UserPrincipal p,@PathVariable Long id,@Valid @RequestBody RefundDecisionRequest b){
  RefundRequest current=refunds.findById(id).orElseThrow(()->err(HttpStatus.NOT_FOUND,"RESOURCE_NOT_FOUND","退款申请不存在"));
  if(!"PENDING".equals(current.getStatus()))throw err(HttpStatus.CONFLICT,"BUSINESS_CONFLICT","退款已处理");
  if(refunds.decideForMerchant(id,p.userId(),b.status())==0)throw err(HttpStatus.FORBIDDEN,"FORBIDDEN","退款申请不属于本店");
  return ApiResponse.success(refunds.findById(id).orElseThrow(()->err(HttpStatus.NOT_FOUND,"RESOURCE_NOT_FOUND","退款申请不存在")));
 }
 private BusinessException err(HttpStatus s,String c,String m){return new BusinessException(s,c,m);}
}
