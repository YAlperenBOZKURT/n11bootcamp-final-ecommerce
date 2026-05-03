package com.yabozkurt.n11bootcamp.ecommerce.product.presentation.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

public class VariantResponse {

    private Long id;
    private Long productId;
    private String sku;
    private Map<String, String> attributes;

    private BigDecimal price; // this is the current selling price
    private BigDecimal effectivePrice; // this is the final price used at checkout (order-service)
    private BigDecimal originalPrice; // this is the pre-discount price (null if no discount)
    private BigDecimal discountRate; // this is the discount percent (e.g. 20 means 20% off)
    private LocalDateTime discountStartAt;
    private LocalDateTime discountEndAt;

    private String status;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }
    public String getSku() { return sku; }
    public void setSku(String sku) { this.sku = sku; }
    public Map<String, String> getAttributes() { return attributes; }
    public void setAttributes(Map<String, String> attributes) { this.attributes = attributes; }
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public BigDecimal getEffectivePrice() { return effectivePrice; }
    public void setEffectivePrice(BigDecimal effectivePrice) { this.effectivePrice = effectivePrice; }
    public BigDecimal getOriginalPrice() { return originalPrice; }
    public void setOriginalPrice(BigDecimal originalPrice) { this.originalPrice = originalPrice; }
    public BigDecimal getDiscountRate() { return discountRate; }
    public void setDiscountRate(BigDecimal discountRate) { this.discountRate = discountRate; }
    public LocalDateTime getDiscountStartAt() { return discountStartAt; }
    public void setDiscountStartAt(LocalDateTime discountStartAt) { this.discountStartAt = discountStartAt; }
    public LocalDateTime getDiscountEndAt() { return discountEndAt; }
    public void setDiscountEndAt(LocalDateTime discountEndAt) { this.discountEndAt = discountEndAt; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
