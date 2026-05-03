package com.yabozkurt.n11bootcamp.ecommerce.cart.infrastructure.client.dto;

import java.math.BigDecimal;

public class CouponValidationData {
    private Long couponId;
    private String code;
    private BigDecimal discountAmount;
    private BigDecimal finalAmount;

    public Long getCouponId() { return couponId; }
    public String getCode() { return code; }
    public BigDecimal getDiscountAmount() { return discountAmount; }
    public BigDecimal getFinalAmount() { return finalAmount; }
}
