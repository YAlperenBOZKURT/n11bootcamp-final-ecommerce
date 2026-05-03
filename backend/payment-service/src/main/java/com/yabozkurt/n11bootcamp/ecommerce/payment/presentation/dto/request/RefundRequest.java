package com.yabozkurt.n11bootcamp.ecommerce.payment.presentation.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public class RefundRequest {
    @NotBlank
    private String orderId;
    @NotNull
    @DecimalMin("0.01")
    private BigDecimal amount;

    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
}
