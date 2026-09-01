package cn.edu.tju.takeout.order;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class Order {
    private Long id;
    private String orderNo;
    private Long userId;
    private Long shopId;
    private BigDecimal totalAmount;
    private String status;
    private LocalDateTime createdAt;

    public static Order pending(
            String orderNo, Long userId, Long shopId, BigDecimal totalAmount) {
        Order order = new Order();
        order.orderNo = orderNo;
        order.userId = userId;
        order.shopId = shopId;
        order.totalAmount = totalAmount;
        order.status = "PENDING";
        order.createdAt = LocalDateTime.now();
        return order;
    }

    public static Order restore(
            Long id, String orderNo, Long userId, Long shopId, BigDecimal totalAmount,
            String status, LocalDateTime createdAt) {
        Order order = new Order();
        order.id = id;
        order.orderNo = orderNo;
        order.userId = userId;
        order.shopId = shopId;
        order.totalAmount = totalAmount;
        order.status = status;
        order.createdAt = createdAt;
        return order;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getOrderNo() { return orderNo; }
    public Long getUserId() { return userId; }
    public Long getShopId() { return shopId; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public String getStatus() { return status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void changeStatus(String status) { this.status = status; }
}
