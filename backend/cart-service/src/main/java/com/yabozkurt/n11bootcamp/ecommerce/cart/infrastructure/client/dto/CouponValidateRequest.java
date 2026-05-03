package com.yabozkurt.n11bootcamp.ecommerce.cart.infrastructure.client.dto;

import java.math.BigDecimal;

public class CouponValidateRequest {
    private String code;
    private BigDecimal orderAmount;

    public CouponValidateRequest(String code, BigDecimal orderAmount) {
        this.code = code;
        this.orderAmount = orderAmount;
    }

    public String getCode() { return code; }
    public BigDecimal getOrderAmount() { return orderAmount; }
}
