package com.yabozkurt.n11bootcamp.ecommerce.order.infrastructure.client.dto;

import java.math.BigDecimal;

public class ValidateCouponRequest {
    private String code;
    private BigDecimal orderAmount;

    public ValidateCouponRequest() {}

    public ValidateCouponRequest(String code, BigDecimal orderAmount) {
        this.code = code;
        this.orderAmount = orderAmount;
    }

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public BigDecimal getOrderAmount() { return orderAmount; }
    public void setOrderAmount(BigDecimal orderAmount) { this.orderAmount = orderAmount; }
}
