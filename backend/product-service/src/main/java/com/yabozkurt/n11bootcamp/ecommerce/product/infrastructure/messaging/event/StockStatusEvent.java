package com.yabozkurt.n11bootcamp.ecommerce.product.infrastructure.messaging.event;

/**
 * stock-service → product-service.
 * when variant stock changes, stock-service publishes this event to notify product-service to update variant status 
 */
public class StockStatusEvent {

    public enum Type { DEPLETED, REPLENISHED }

    private Long productId;
    private Long variantId;   // null → product-level stock
    private Type type;
    private int availableQuantity;

    public StockStatusEvent() {}

    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }
    public Long getVariantId() { return variantId; }
    public void setVariantId(Long variantId) { this.variantId = variantId; }
    public Type getType() { return type; }
    public void setType(Type type) { this.type = type; }
    public int getAvailableQuantity() { return availableQuantity; }
    public void setAvailableQuantity(int availableQuantity) { this.availableQuantity = availableQuantity; }
}
