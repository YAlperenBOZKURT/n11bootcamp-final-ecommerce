package com.yabozkurt.n11bootcamp.ecommerce.product.infrastructure.elasticsearch.document;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Elasticsearch index mapping for products.
 *
 * name / description / brand → text (full-text search)
 * status / categoryId / attributes.* → keyword (exact filter)
 * price / discountRate → double (range queries)
 *
 * Attributes are stored as a dynamic object so ES auto-maps
 * each key as a keyword sub-field (e.g. attributes.color).
 * This lets us do term-filter: attributes.color = "Siyah".
 */
@Document(indexName = "products")
@Setting(settingPath = "/elasticsearch/product-settings.json")
public class ProductDocument {

    @Id
    private String id;

    @MultiField(
        mainField  = @Field(type = FieldType.Text,    analyzer = "standard"),
        otherFields = @InnerField(suffix = "keyword", type = FieldType.Keyword)
    )
    private String name;

    @Field(type = FieldType.Text)
    private String description;

    @MultiField(
        mainField  = @Field(type = FieldType.Text,    analyzer = "standard"),
        otherFields = @InnerField(suffix = "keyword", type = FieldType.Keyword)
    )
    private String brand;

    @Field(type = FieldType.Long)
    private Long categoryId;

    @Field(type = FieldType.Keyword)
    private String categoryName;

    @Field(type = FieldType.Keyword)
    private String status;

    @Field(type = FieldType.Double)
    private BigDecimal priceFrom;

    @Field(type = FieldType.Keyword)
    private String imageUrl;

    
    @Field(type = FieldType.Object)
    private Map<String, String> attributes;

    @Field(type = FieldType.Keyword)
    private List<String> variantAttributeValues = new ArrayList<>();

    @Field(type = FieldType.Date, format = {DateFormat.date_hour_minute_second_millis, DateFormat.epoch_millis})
    private LocalDateTime createdAt;

    // -- Constructors ----------------------------------------------------------

    public ProductDocument() {}

    // -- Getters / Setters -----------------------------------------------------

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getBrand() { return brand; }
    public void setBrand(String brand) { this.brand = brand; }

    public Long getCategoryId() { return categoryId; }
    public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }

    public String getCategoryName() { return categoryName; }
    public void setCategoryName(String categoryName) { this.categoryName = categoryName; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public BigDecimal getPriceFrom() { return priceFrom; }
    public void setPriceFrom(BigDecimal priceFrom) { this.priceFrom = priceFrom; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public Map<String, String> getAttributes() { return attributes; }
    public void setAttributes(Map<String, String> attributes) { this.attributes = attributes; }

    public List<String> getVariantAttributeValues() { return variantAttributeValues; }
    public void setVariantAttributeValues(List<String> variantAttributeValues) { this.variantAttributeValues = variantAttributeValues; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
