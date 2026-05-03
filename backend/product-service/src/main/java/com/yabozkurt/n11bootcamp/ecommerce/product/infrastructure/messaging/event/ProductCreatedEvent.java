package com.yabozkurt.n11bootcamp.ecommerce.product.infrastructure.messaging.event;

public class ProductCreatedEvent {

    private Long productId;
    private String productName;

    public ProductCreatedEvent() {}

    public ProductCreatedEvent(Long productId, String productName) {
        this.productId = productId;
        this.productName = productName;
    }

    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }
    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }
}
