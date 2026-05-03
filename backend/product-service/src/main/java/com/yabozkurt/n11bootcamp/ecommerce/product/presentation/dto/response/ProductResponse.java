package com.yabozkurt.n11bootcamp.ecommerce.product.presentation.dto.response;

import com.yabozkurt.n11bootcamp.ecommerce.product.domain.model.enums.ProductStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public class ProductResponse {
    private Long id;
    private String name;
    private String description;
    private String brand;
    private String imageUrl;
    private List<String> imageUrls;
    private Map<String, String> attributes;
    private BigDecimal priceFrom; // this is the lowest effective price among variants
    private boolean inStock; // this is true if at least 1 variant is ACTIVE for showing "In Stock" badge in the UI
    private ProductStatus status;
    private CategoryResponse category;
    private List<VariantResponse> variants;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getBrand() { return brand; }
    public void setBrand(String brand) { this.brand = brand; }
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public List<String> getImageUrls() { return imageUrls; }
    public void setImageUrls(List<String> imageUrls) { this.imageUrls = imageUrls; }
    public Map<String, String> getAttributes() { return attributes; }
    public void setAttributes(Map<String, String> attributes) { this.attributes = attributes; }
    public BigDecimal getPriceFrom() { return priceFrom; }
    public void setPriceFrom(BigDecimal priceFrom) { this.priceFrom = priceFrom; }
    public boolean isInStock() { return inStock; }
    public void setInStock(boolean inStock) { this.inStock = inStock; }
    public ProductStatus getStatus() { return status; }
    public void setStatus(ProductStatus status) { this.status = status; }
    public CategoryResponse getCategory() { return category; }
    public void setCategory(CategoryResponse category) { this.category = category; }
    public List<VariantResponse> getVariants() { return variants; }
    public void setVariants(List<VariantResponse> variants) { this.variants = variants; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
