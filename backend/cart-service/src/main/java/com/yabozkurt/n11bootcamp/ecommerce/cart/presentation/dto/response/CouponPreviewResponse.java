package com.yabozkurt.n11bootcamp.ecommerce.cart.presentation.dto.response;

import java.math.BigDecimal;

public class CouponPreviewResponse {
    private String code;
    private BigDecimal cartTotal;
    private BigDecimal discountAmount;
    private BigDecimal finalAmount;

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public BigDecimal getCartTotal() { return cartTotal; }
    public void setCartTotal(BigDecimal cartTotal) { this.cartTotal = cartTotal; }
    public BigDecimal getDiscountAmount() { return discountAmount; }
    public void setDiscountAmount(BigDecimal discountAmount) { this.discountAmount = discountAmount; }
    public BigDecimal getFinalAmount() { return finalAmount; }
    public void setFinalAmount(BigDecimal finalAmount) { this.finalAmount = finalAmount; }
}
