package com.yabozkurt.n11bootcamp.ecommerce.product.presentation.dto.request;

import jakarta.validation.constraints.*;

import java.util.List;

public class ProductRequest {

    @NotBlank
    @Size(max = 255)
    private String name;

    @NotBlank
    private String description;

    @NotBlank
    @Size(max = 100)
    private String brand;

    @NotNull
    private Long categoryId;

    // Max 3 images allowed for a product
    @Size(max = 3)
    private List<String> imageUrls;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getBrand() { return brand; }
    public void setBrand(String brand) { this.brand = brand; }
    public Long getCategoryId() { return categoryId; }
    public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }
    public List<String> getImageUrls() { return imageUrls; }
    public void setImageUrls(List<String> imageUrls) { this.imageUrls = imageUrls; }
}
