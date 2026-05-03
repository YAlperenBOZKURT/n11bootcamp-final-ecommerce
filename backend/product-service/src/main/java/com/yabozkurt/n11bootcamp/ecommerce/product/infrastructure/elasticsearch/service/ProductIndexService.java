package com.yabozkurt.n11bootcamp.ecommerce.product.infrastructure.elasticsearch.service;

import co.elastic.clients.elasticsearch._types.query_dsl.BoolQuery;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import com.yabozkurt.n11bootcamp.ecommerce.product.domain.model.Product;
import com.yabozkurt.n11bootcamp.ecommerce.product.infrastructure.elasticsearch.document.ProductDocument;
import com.yabozkurt.n11bootcamp.ecommerce.product.infrastructure.elasticsearch.repository.ProductSearchRepository;
import com.yabozkurt.n11bootcamp.ecommerce.product.presentation.dto.response.CategoryResponse;
import com.yabozkurt.n11bootcamp.ecommerce.product.presentation.dto.response.ProductResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.stereotype.Service;

import com.yabozkurt.n11bootcamp.ecommerce.product.domain.model.enums.VariantStatus;

import java.util.List;
import java.util.Map;

@Service
public class ProductIndexService {

    private static final Logger log = LoggerFactory.getLogger(ProductIndexService.class);

    private final ProductSearchRepository searchRepository;
    private final ElasticsearchOperations esOps;

    public ProductIndexService(ProductSearchRepository searchRepository,
                               ElasticsearchOperations esOps) {
        this.searchRepository = searchRepository;
        this.esOps = esOps;
    }

    // -- Index operations ------------------------------------------------------

    public void index(Product product) {
        try {
            searchRepository.save(toDocument(product));
        } catch (Exception ex) {
            log.warn("ES index failed for product {}: {}", product.getId(), ex.getMessage());
        }
    }

    public void updateStatus(Long productId, String status) {
        try {
            searchRepository.findById(String.valueOf(productId)).ifPresent(doc -> {
                doc.setStatus(status);
                searchRepository.save(doc);
            });
        } catch (Exception ex) {
            log.warn("ES status update failed for product {}: {}", productId, ex.getMessage());
        }
    }

    public void delete(Long productId) {
        try {
            searchRepository.deleteById(String.valueOf(productId));
        } catch (Exception ex) {
            log.warn("ES delete failed for product {}: {}", productId, ex.getMessage());
        }
    }

    public void bulkIndex(List<Product> products) {
        try {
            List<ProductDocument> docs = products.stream().map(this::toDocument).toList();
            searchRepository.saveAll(docs);
            log.info("ES bulk indexed {} products", docs.size());
        } catch (Exception ex) {
            log.warn("ES bulk index failed: {}", ex.getMessage());
        }
    }

    // -- Search ----------------------------------------------------------------

    /**
     * Full-text + filter search via Elasticsearch.
     *
     * Query structure:
     *   bool {
     *     must:   term(status, ACTIVE)
     *     should: multiMatch(keyword → name^3, brand^2, description)   [if keyword]
     *     filter: term(categoryId, ?)                                  [if categoryId]
     *     filter: term(attributes.KEY, VALUE)                          [per attribute]
     *   }
     */

    public Page<ProductResponse> search(String keyword,
                                        Long categoryId,
                                        Map<String, String> attributes,
                                        Pageable pageable) {
        BoolQuery.Builder bool = new BoolQuery.Builder();

        // Always filter by ACTIVE status
        bool.must(Query.of(q -> q.term(t -> t.field("status").value("ACTIVE"))));

        // Full-text search on name (boosted), brand, description
        if (keyword != null && !keyword.isBlank()) {
            String kw = keyword.trim();
            bool.should(Query.of(q -> q.multiMatch(m -> m
                    .query(kw)
                    // give higher relevance to name and brand matches vs description 
                    .fields("name^3", "brand^2", "description")
                    // tries to tolerate typos and close misspellings
                    .fuzziness("AUTO")
            )));
            bool.minimumShouldMatch("1");
        }

        // Category filter
        if (categoryId != null) {
            long catId = categoryId;
            bool.filter(Query.of(q -> q.term(t -> t.field("categoryId").value(catId))));
        }

        // Attribute filters: term("variantAttributeValues", "Key:Value")
        if (attributes != null) {
            attributes.forEach((key, value) -> {
                if (key != null && !key.isBlank() && value != null && !value.isBlank()) {
                    String attrTerm = key + ":" + value;
                    bool.filter(Query.of(q -> q.term(t -> t
                            .field("variantAttributeValues")
                            .value(attrTerm)
                    )));
                }
            });
        }

        NativeQuery query = NativeQuery.builder()
                .withQuery(Query.of(q -> q.bool(bool.build())))
                .withPageable(pageable)
                .build();

        SearchHits<ProductDocument> hits = esOps.search(query, ProductDocument.class);

        List<ProductResponse> results = hits.getSearchHits().stream()
                .map(SearchHit::getContent)
                .map(this::docToResponse)
                .toList();

        return new PageImpl<>(results, pageable, hits.getTotalHits());
    }

    // -- Conversion ------------------------------------------------------------

    public ProductDocument toDocument(Product p) {
        ProductDocument doc = new ProductDocument();
        doc.setId(String.valueOf(p.getId()));
        doc.setName(p.getName());
        doc.setDescription(p.getDescription());
        doc.setBrand(p.getBrand());
        List<String> imgs = p.getImageUrls();
        doc.setImageUrl(imgs.isEmpty() ? null : imgs.get(0));
        doc.setAttributes(p.getAttributes());
        doc.setStatus(p.getStatus() != null ? p.getStatus().name() : null);
        doc.setCreatedAt(p.getCreatedAt());
        if (p.getVariants() != null) {
            p.getVariants().stream()
                    .filter(v -> v.getStatus() == VariantStatus.ACTIVE)
                    .forEach(v -> {
                        // Set priceFrom to the lowest price among ACTIVE variants
                        if (doc.getPriceFrom() == null ||
                                (v.getPrice() != null && v.getPrice().compareTo(doc.getPriceFrom()) < 0)) {
                            doc.setPriceFrom(v.getPrice());
                        }
                        // "Key:Value" entries for attribute term filtering
                        if (v.getAttributes() != null) {
                            v.getAttributes().forEach((key, value) ->
                                    doc.getVariantAttributeValues().add(key + ":" + value));
                        }
                    });
            // Deduplicate
            doc.setVariantAttributeValues(doc.getVariantAttributeValues().stream().distinct().toList());
        }
        if (p.getCategory() != null) {
            doc.setCategoryId(p.getCategory().getId());
            doc.setCategoryName(p.getCategory().getName());
        }
        return doc;
    }

    private ProductResponse docToResponse(ProductDocument doc) {
        ProductResponse res = new ProductResponse();
        res.setId(Long.parseLong(doc.getId()));
        res.setName(doc.getName());
        res.setDescription(doc.getDescription());
        res.setBrand(doc.getBrand());
        res.setPriceFrom(doc.getPriceFrom());
        res.setImageUrl(doc.getImageUrl());
        res.setAttributes(doc.getAttributes());
        res.setInStock("ACTIVE".equals(doc.getStatus()));

        if (doc.getCategoryId() != null) {
            CategoryResponse cat = new CategoryResponse();
            cat.setId(doc.getCategoryId());
            cat.setName(doc.getCategoryName());
            res.setCategory(cat);
        }
        return res;
    }
}
