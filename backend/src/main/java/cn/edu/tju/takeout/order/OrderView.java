package cn.edu.tju.takeout.order;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record OrderView(
        Long id, String orderNo, Long shopId, BigDecimal totalAmount, String status,
        LocalDateTime createdAt, List<OrderItemView> items,
        /* 店铺照片，只在订单详情下发；其他出口为 null，前端退化成占位图。 */
        String shopImage,
        String shopPhone, String userPhoneMasked, String userAddress,
        String recipientName, String recipientPhoneMasked, String deliveryAddress,
        String paymentStatus, LocalDateTime paymentDeadline, LocalDateTime paidAt, Long riderId) {

    public OrderView(
            Long id, String orderNo, Long shopId, BigDecimal totalAmount, String status,
            LocalDateTime createdAt, List<OrderItemView> items) {
        this(id, orderNo, shopId, totalAmount, status, createdAt, items, null, null, null, null,
                null, null, null, null, null, null, null);
    }

    public static OrderView from(Order order, List<OrderItem> items) {
        return new OrderView(
                order.getId(), order.getOrderNo(), order.getShopId(), order.getTotalAmount(),
                order.getStatus(), order.getCreatedAt(), items.stream().map(OrderItemView::from).toList(),
                null, null, null, null, order.getRecipientName(), mask(order.getRecipientPhone()),
                order.getDeliveryAddress(), order.getPaymentStatus(), order.getPaymentDeadline(),
                order.getPaidAt(), order.getRiderId());
    }

    public OrderView withContacts(
            String shopPhone, String userPhoneMasked, String userAddress, String shopImage) {
        return new OrderView(
                id, orderNo, shopId, totalAmount, status, createdAt, items, shopImage,
                shopPhone, userPhoneMasked, userAddress, recipientName, recipientPhoneMasked,
                deliveryAddress, paymentStatus, paymentDeadline, paidAt, riderId);
    }

    private static String mask(String phone) {
        if (phone == null || phone.length() < 7) return phone;
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }
}
