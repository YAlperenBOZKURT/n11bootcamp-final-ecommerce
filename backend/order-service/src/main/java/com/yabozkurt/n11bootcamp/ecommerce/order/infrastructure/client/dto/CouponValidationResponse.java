package com.yabozkurt.n11bootcamp.ecommerce.order.infrastructure.client.dto;

import java.math.BigDecimal;

public class CouponValidationResponse {
    private BigDecimal discountAmount;
    private BigDecimal finalAmount;

    public BigDecimal getDiscountAmount() { return discountAmount; }
    public void setDiscountAmount(BigDecimal discountAmount) { this.discountAmount = discountAmount; }
    public BigDecimal getFinalAmount() { return finalAmount; }
    public void setFinalAmount(BigDecimal finalAmount) { this.finalAmount = finalAmount; }
}
