package com.yabozkurt.n11bootcamp.stock.infrastructure.messaging.event;

public class StockStatusEvent {

    public enum Type { DEPLETED, REPLENISHED }

    private Long productId;
    private Long variantId;
    private Type type;
    private int availableQuantity;

    public StockStatusEvent() {}

    public StockStatusEvent(Long productId, Long variantId, Type type, int availableQuantity) {
        this.productId = productId;
        this.variantId = variantId;
        this.type = type;
        this.availableQuantity = availableQuantity;
    }

    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }
    public Long getVariantId() { return variantId; }
    public void setVariantId(Long variantId) { this.variantId = variantId; }
    public Type getType() { return type; }
    public void setType(Type type) { this.type = type; }
    public int getAvailableQuantity() { return availableQuantity; }
    public void setAvailableQuantity(int availableQuantity) { this.availableQuantity = availableQuantity; }
}
