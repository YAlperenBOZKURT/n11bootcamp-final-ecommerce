package com.yabozkurt.n11bootcamp.ecommerce.order.infrastructure.messaging.event;

import java.math.BigDecimal;

public class OrderConfirmedEvent {
    private String orderId;
    private Long userId;
    private String userEmail;
    private BigDecimal totalAmount;

    public OrderConfirmedEvent() {}

    public OrderConfirmedEvent(String orderId, Long userId, String userEmail, BigDecimal totalAmount) {
        this.orderId = orderId;
        this.userId = userId;
        this.userEmail = userEmail;
        this.totalAmount = totalAmount;
    }

    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getUserEmail() { return userEmail; }
    public void setUserEmail(String userEmail) { this.userEmail = userEmail; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
}
