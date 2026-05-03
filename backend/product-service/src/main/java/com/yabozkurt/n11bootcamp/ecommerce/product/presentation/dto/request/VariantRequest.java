package com.yabozkurt.n11bootcamp.ecommerce.product.presentation.dto.request;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

public class VariantRequest {

    @NotBlank
    private String sku;

    // At least 1 attribute is required (e.g. size=M, color=Black). 
    @NotEmpty(message = "Varyant için en az bir özellik (attribute) girilmelidir.")
    private Map<String, String> attributes;

    @NotNull
    @DecimalMin("0.01")
    private BigDecimal price;

    @DecimalMin("0")
    @DecimalMax("100")
    private BigDecimal discountRate;

    private LocalDateTime discountStartAt;
    private LocalDateTime discountEndAt;

    public String getSku() { return sku; }
    public void setSku(String sku) { this.sku = sku; }
    public Map<String, String> getAttributes() { return attributes; }
    public void setAttributes(Map<String, String> attributes) { this.attributes = attributes; }
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public BigDecimal getDiscountRate() { return discountRate; }
    public void setDiscountRate(BigDecimal discountRate) { this.discountRate = discountRate; }
    public LocalDateTime getDiscountStartAt() { return discountStartAt; }
    public void setDiscountStartAt(LocalDateTime discountStartAt) { this.discountStartAt = discountStartAt; }
    public LocalDateTime getDiscountEndAt() { return discountEndAt; }
    public void setDiscountEndAt(LocalDateTime discountEndAt) { this.discountEndAt = discountEndAt; }
}
