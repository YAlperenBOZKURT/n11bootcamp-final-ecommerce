package com.yabozkurt.n11bootcamp.ecommerce.cart.infrastructure.messaging.event;

public class OrderConfirmedEvent {

    private String orderId;
    private Long userId;

    public OrderConfirmedEvent() {}

    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
}
