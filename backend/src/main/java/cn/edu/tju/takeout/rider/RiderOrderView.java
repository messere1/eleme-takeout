package cn.edu.tju.takeout.rider;
import cn.edu.tju.takeout.order.Order;
import java.math.BigDecimal;
import java.time.LocalDateTime;
public record RiderOrderView(Long id,String orderNo,Long shopId,BigDecimal totalAmount,String status,
        String paymentStatus,LocalDateTime createdAt,String recipientName,String recipientPhone,String deliveryAddress){
    static RiderOrderView available(Order o){return new RiderOrderView(o.getId(),o.getOrderNo(),o.getShopId(),o.getTotalAmount(),o.getStatus(),o.getPaymentStatus(),o.getCreatedAt(),null,null,null);}
    static RiderOrderView assigned(Order o){return new RiderOrderView(o.getId(),o.getOrderNo(),o.getShopId(),o.getTotalAmount(),o.getStatus(),o.getPaymentStatus(),o.getCreatedAt(),o.getRecipientName(),o.getRecipientPhone(),o.getDeliveryAddress());}
}
