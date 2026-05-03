package com.yabozkurt.n11bootcamp.stock.infrastructure.messaging.event;

public class VariantCreatedEvent {

    private Long productId;
    private Long variantId;

    public VariantCreatedEvent() {}

    public VariantCreatedEvent(Long productId, Long variantId) {
        this.productId = productId;
        this.variantId = variantId;
    }

    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }
    public Long getVariantId() { return variantId; }
    public void setVariantId(Long variantId) { this.variantId = variantId; }
}
