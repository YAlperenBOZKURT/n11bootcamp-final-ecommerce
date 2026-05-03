package com.yabozkurt.n11bootcamp.ecommerce.payment.infrastructure.messaging.event;

import java.math.BigDecimal;

public class PaymentCompletedEvent {

    private String orderId;
    private Long userId;
    private BigDecimal amount;
    private String provider;
    private String providerPaymentId;

    public PaymentCompletedEvent() {}

    public PaymentCompletedEvent(String orderId, Long userId, BigDecimal amount,
                                  String provider, String providerPaymentId) {
        this.orderId = orderId;
        this.userId = userId;
        this.amount = amount;
        this.provider = provider;
        this.providerPaymentId = providerPaymentId;
    }

    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }

    public String getProviderPaymentId() { return providerPaymentId; }
    public void setProviderPaymentId(String providerPaymentId) { this.providerPaymentId = providerPaymentId; }
}
