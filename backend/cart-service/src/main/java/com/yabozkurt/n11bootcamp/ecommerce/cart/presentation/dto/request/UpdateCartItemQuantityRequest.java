package com.yabozkurt.n11bootcamp.ecommerce.cart.presentation.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public class UpdateCartItemQuantityRequest {
    @NotNull
    @Min(1)
    private Integer quantity;

    public Integer getQuantity() { return quantity; }
}
