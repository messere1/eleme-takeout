package cn.edu.tju.takeout.admin;

import cn.edu.tju.takeout.order.Order;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 管理员订单列表项。
 *
 * <p>此前该接口直接序列化 {@link Order} 实体，把收件人手机号和收货地址明文吐了出去，
 * 违反 SRS V2.0 NFR-004「列表手机号脱敏；履约地址仅最小必要角色可见」——管理员是治理角色，
 * 不属于履约角色。这里只保留治理所需的字段，手机号脱敏、地址不下发。
 */
public record AdminOrderView(
        Long id, String orderNo, Long userId, Long shopId, BigDecimal totalAmount,
        String status, String paymentStatus, LocalDateTime paymentDeadline,
        LocalDateTime createdAt, String recipientPhoneMasked) {

    public static AdminOrderView from(Order order, String maskedPhone) {
        return new AdminOrderView(
                order.getId(), order.getOrderNo(), order.getUserId(), order.getShopId(),
                order.getTotalAmount(), order.getStatus(), order.getPaymentStatus(),
                order.getPaymentDeadline(), order.getCreatedAt(), maskedPhone);
    }
}
