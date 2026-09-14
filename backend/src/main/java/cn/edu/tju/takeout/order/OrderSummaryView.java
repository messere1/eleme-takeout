package cn.edu.tju.takeout.order;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单列表摘要。
 *
 * <p>带上 {@code paymentStatus} 与 {@code paymentDeadline}，因为 SRS V2.0 的订单状态是
 * 「业务状态 + 支付状态」这一对（§5.1）：{@code CREATED+UNPAID} 与 {@code CREATED+PAID}
 * 允许的动作不同（前者可支付、可取消，后者只能由商家接单或申请退款）。支付后业务状态仍是
 * {@code CREATED}，列表若只给 status，前端无法区分这两种状态。
 *
 * <p>{@code paymentDeadline} 同理：§9.1 要求「支付倒计时以 paymentDeadline 为准」，
 * FR-019 要求「页面依据服务器截止时间同步」，客户端不能拿 createdAt 自己加 15 分钟推算。
 */
public record OrderSummaryView(
        Long id, String orderNo, Long shopId, BigDecimal totalAmount,
        String status, String paymentStatus, LocalDateTime paymentDeadline,
        LocalDateTime createdAt) {
    public static OrderSummaryView from(Order order) {
        return new OrderSummaryView(
                order.getId(), order.getOrderNo(), order.getShopId(), order.getTotalAmount(),
                order.getStatus(), order.getPaymentStatus(), order.getPaymentDeadline(),
                order.getCreatedAt());
    }
}
