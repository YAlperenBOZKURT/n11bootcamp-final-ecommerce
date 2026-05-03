package com.yabozkurt.n11bootcamp.ecommerce.order.infrastructure.client.dto;

import java.math.BigDecimal;
import java.util.List;

public class ProductClientResponse {
    private Long id;
    private String name;
    private BigDecimal price;
    private List<VariantClientResponse> variants;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public List<VariantClientResponse> getVariants() { return variants; }
    public void setVariants(List<VariantClientResponse> variants) { this.variants = variants; }
}
