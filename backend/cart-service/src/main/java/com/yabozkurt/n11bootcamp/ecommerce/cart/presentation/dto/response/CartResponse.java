package com.yabozkurt.n11bootcamp.ecommerce.cart.presentation.dto.response;

import java.math.BigDecimal;
import java.util.List;

public class CartResponse {
    private Long userId;
    private List<CartItemResponse> items;
    private BigDecimal totalAmount;

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public List<CartItemResponse> getItems() { return items; }
    public void setItems(List<CartItemResponse> items) { this.items = items; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
}
