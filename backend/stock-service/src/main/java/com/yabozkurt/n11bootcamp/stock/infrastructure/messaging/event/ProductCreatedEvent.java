package com.yabozkurt.n11bootcamp.stock.infrastructure.messaging.event;

public class ProductCreatedEvent {
    private Long productId;
    private String productName;

    public ProductCreatedEvent() {}

    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }
    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }
}
