package com.yabozkurt.n11bootcamp.coupon.presentation.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public class ValidateCouponRequest {

    @NotBlank
    private String code;

    @NotNull
    @DecimalMin(value = "0.00")
    private BigDecimal orderAmount;

    public String getCode() {
        return code;
    }

    public BigDecimal getOrderAmount() {
        return orderAmount;
    }
}
