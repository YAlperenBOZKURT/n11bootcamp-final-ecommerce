package com.yabozkurt.n11bootcamp.ecommerce.order.infrastructure.messaging.event;

public class OrderCancelledEvent {
    private String orderId;
    private Long userId;
    private String userEmail;
    private String reason;

    public OrderCancelledEvent() {}

    public OrderCancelledEvent(String orderId, Long userId, String userEmail, String reason) {
        this.orderId = orderId;
        this.userId = userId;
        this.userEmail = userEmail;
        this.reason = reason;
    }

    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getUserEmail() { return userEmail; }
    public void setUserEmail(String userEmail) { this.userEmail = userEmail; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}
