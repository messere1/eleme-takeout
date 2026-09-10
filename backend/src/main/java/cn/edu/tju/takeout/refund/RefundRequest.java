package cn.edu.tju.takeout.refund;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class RefundRequest {
    private Long id; private Long orderId; private Long userId; private BigDecimal amount;
    private String reason; private String evidenceUrls; private String status; private LocalDateTime createdAt; private LocalDateTime handledAt;
    public static RefundRequest pending(Long orderId,Long userId,BigDecimal amount,String reason,String evidence){
        RefundRequest r=new RefundRequest();r.orderId=orderId;r.userId=userId;r.amount=amount;r.reason=reason;
        r.evidenceUrls=evidence;r.status="PENDING";r.createdAt=LocalDateTime.now();return r;}
    public Long getId(){return id;} public void setId(Long id){this.id=id;} public Long getOrderId(){return orderId;}
    public Long getUserId(){return userId;} public BigDecimal getAmount(){return amount;} public String getReason(){return reason;}
    public String getEvidenceUrls(){return evidenceUrls;} public String getStatus(){return status;}
    public LocalDateTime getCreatedAt(){return createdAt;} public LocalDateTime getHandledAt(){return handledAt;}
}
