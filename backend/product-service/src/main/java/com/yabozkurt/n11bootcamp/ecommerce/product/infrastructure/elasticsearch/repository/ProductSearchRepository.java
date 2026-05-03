package com.yabozkurt.n11bootcamp.ecommerce.product.infrastructure.elasticsearch.repository;

import com.yabozkurt.n11bootcamp.ecommerce.product.infrastructure.elasticsearch.document.ProductDocument;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

// Spring Data Elasticsearch repository for ProductDocument.

public interface ProductSearchRepository extends ElasticsearchRepository<ProductDocument, String> {
}
