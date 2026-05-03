package com.yabozkurt.n11bootcamp.ecommerce.order.infrastructure.client.dto;

import java.math.BigDecimal;

public class UseCouponRequest {
    private String code;
    private String orderId;
    private BigDecimal orderAmount;

    public UseCouponRequest() {}

    public UseCouponRequest(String code, String orderId, BigDecimal orderAmount) {
        this.code = code;
        this.orderId = orderId;
        this.orderAmount = orderAmount;
    }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }
    public BigDecimal getOrderAmount() { return orderAmount; }
    public void setOrderAmount(BigDecimal orderAmount) { this.orderAmount = orderAmount; }
}
