package com.yabozkurt.n11bootcamp.ecommerce.payment.infrastructure.messaging.event;

import java.math.BigDecimal;

public class PaymentFailedEvent {

    private String orderId;
    private Long userId;
    private BigDecimal amount;
    private String failReason;

    public PaymentFailedEvent() {}

    public PaymentFailedEvent(String orderId, Long userId, BigDecimal amount, String failReason) {
        this.orderId = orderId;
        this.userId = userId;
        this.amount = amount;
        this.failReason = failReason;
    }

    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public String getFailReason() { return failReason; }
    public void setFailReason(String failReason) { this.failReason = failReason; }
}
