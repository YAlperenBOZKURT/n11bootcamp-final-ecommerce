package com.yabozkurt.n11bootcamp.coupon.presentation.dto.request;

import jakarta.validation.constraints.NotBlank;

public class ClaimCouponRequest {
    @NotBlank
    private String code;

    public String getCode() {
        return code;
    }
}
