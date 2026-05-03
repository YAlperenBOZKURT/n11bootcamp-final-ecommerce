package com.yabozkurt.n11bootcamp.ecommerce.order.presentation.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public class OrderRequest {

    @NotNull
    @NotEmpty
    @Valid
    private List<OrderItemRequest> items;

    @Valid
    @NotNull
    private CardRequest card;

    private String couponCode;

    public List<OrderItemRequest> getItems() { return items; }
    public void setItems(List<OrderItemRequest> items) { this.items = items; }
    public CardRequest getCard() { return card; }
    public void setCard(CardRequest card) { this.card = card; }
    public String getCouponCode() { return couponCode; }
    public void setCouponCode(String couponCode) { this.couponCode = couponCode; }
}
