package com.yabozkurt.n11bootcamp.ecommerce.order.infrastructure.client.dto;

import java.math.BigDecimal;

public class VariantClientResponse {
    private Long id;
    private BigDecimal effectivePrice;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public BigDecimal getEffectivePrice() { return effectivePrice; }
    public void setEffectivePrice(BigDecimal effectivePrice) { this.effectivePrice = effectivePrice; }
}
