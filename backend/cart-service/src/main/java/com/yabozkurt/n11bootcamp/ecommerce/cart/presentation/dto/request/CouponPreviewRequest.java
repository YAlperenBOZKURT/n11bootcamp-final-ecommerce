package com.yabozkurt.n11bootcamp.ecommerce.cart.presentation.dto.request;

import jakarta.validation.constraints.NotBlank;

public class CouponPreviewRequest {
    @NotBlank
    private String code;

    public String getCode() { return code; }
}
