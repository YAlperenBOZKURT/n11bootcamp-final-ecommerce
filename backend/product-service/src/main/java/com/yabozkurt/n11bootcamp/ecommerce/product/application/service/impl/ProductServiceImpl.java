package com.yabozkurt.n11bootcamp.ecommerce.product.application.service.impl;

import com.yabozkurt.n11bootcamp.ecommerce.product.application.service.ProductService;
import com.yabozkurt.n11bootcamp.ecommerce.product.domain.exception.CategoryNotFoundException;
import com.yabozkurt.n11bootcamp.ecommerce.product.domain.exception.ProductNotFoundException;
import com.yabozkurt.n11bootcamp.ecommerce.product.domain.model.Category;
import com.yabozkurt.n11bootcamp.ecommerce.product.domain.model.Product;
import com.yabozkurt.n11bootcamp.ecommerce.product.domain.model.ProductVariant;
import com.yabozkurt.n11bootcamp.ecommerce.product.domain.model.enums.ProductStatus;
import com.yabozkurt.n11bootcamp.ecommerce.product.domain.repository.CategoryRepository;
import com.yabozkurt.n11bootcamp.ecommerce.product.domain.repository.ProductRepository;
import com.yabozkurt.n11bootcamp.ecommerce.product.domain.repository.ProductVariantRepository;
import com.yabozkurt.n11bootcamp.ecommerce.product.infrastructure.elasticsearch.service.ProductIndexService;
import com.yabozkurt.n11bootcamp.ecommerce.product.infrastructure.messaging.event.ProductCreatedEvent;
import com.yabozkurt.n11bootcamp.ecommerce.product.infrastructure.messaging.publisher.ProductEventPublisher;
import com.yabozkurt.n11bootcamp.ecommerce.product.infrastructure.minio.MinioService;
import com.yabozkurt.n11bootcamp.ecommerce.product.presentation.dto.request.ProductRequest;
import com.yabozkurt.n11bootcamp.ecommerce.product.presentation.dto.response.ProductResponse;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class ProductServiceImpl implements ProductService {

    private static final Logger log = LoggerFactory.getLogger(ProductServiceImpl.class);

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final ProductVariantRepository variantRepository;
    private final MinioService minioService;
    private final ProductEventPublisher eventPublisher;
    private final ProductIndexService productIndexService;

    @PersistenceContext
    private EntityManager em;

    public ProductServiceImpl(ProductRepository productRepository,
                              CategoryRepository categoryRepository,
                              ProductVariantRepository variantRepository,
                              MinioService minioService,
                              ProductEventPublisher eventPublisher,
                              ProductIndexService productIndexService) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.variantRepository = variantRepository;
        this.minioService = minioService;
        this.eventPublisher = eventPublisher;
        this.productIndexService = productIndexService;
    }

    @Override
    public ProductResponse getById(Long id) {
        return toResponse(findActive(id));
    }

    @Override
    public Page<ProductResponse> getAll(Pageable pageable) {
        Page<Long> idPage = productRepository.findIdsByStatus(ProductStatus.ACTIVE, pageable);
        return mapIdPageToResponses(idPage, pageable);
    }

    @Override
    public Page<ProductResponse> getByCategory(Long categoryId, Pageable pageable) {
        Page<Long> idPage = productRepository.findIdsByCategoryIdAndStatus(categoryId, ProductStatus.ACTIVE, pageable);
        return mapIdPageToResponses(idPage, pageable);
    }

    @Override
    @SuppressWarnings("unchecked")
    public Page<ProductResponse> search(String keyword, Long categoryId,
                                        Map<String, String> attributes, Pageable pageable) {
        Map<String, String> attrFilters = stripReservedParams(attributes);

        // Only route through ES when there's an actual search term.
        // Pure browsing (no keyword) always hits the DB so every product is visible
        // regardless of whether the ES index is populated.
        if (keyword != null && !keyword.isBlank()) {
            try {
                Page<ProductResponse> esResult = productIndexService.search(keyword, categoryId, attrFilters, pageable);
                if (esResult.getTotalElements() > 0) return esResult;
                log.warn("ES returned 0 results for keyword='{}', falling back to DB", keyword);
            } catch (Exception ex) {
                log.warn("ES search failed, falling back to DB: {}", ex.getMessage());
            }
        }

        // -- PostgreSQL path -------------------------------------------------------
        // Native SQL allows JSONB EXISTS subqueries on product_variants for attribute filtering.
        StringBuilder idSql = new StringBuilder(
                "SELECT p.id FROM products p WHERE p.status = 'ACTIVE'");
        Map<String, Object> params = new HashMap<>();

        if (keyword != null && !keyword.isBlank()) {
            idSql.append(" AND (LOWER(p.name) LIKE :kw OR LOWER(p.brand) LIKE :kw)");
            params.put("kw", "%" + keyword.toLowerCase() + "%");
        }
        if (categoryId != null) {
            idSql.append(" AND p.category_id = :catId");
            params.put("catId", categoryId);
        }
        if (attrFilters != null && !attrFilters.isEmpty()) {
            int i = 0;
            for (Map.Entry<String, String> e : attrFilters.entrySet()) {
                String kp = "ak" + i, vp = "av" + i;
                idSql.append(" AND EXISTS (SELECT 1 FROM product_variants pv")
                     .append(" WHERE pv.product_id = p.id AND pv.status = 'ACTIVE'")
                     .append(" AND jsonb_extract_path_text(pv.attributes, :").append(kp).append(")")
                     .append(" = :").append(vp).append(")");
                params.put(kp, e.getKey());
                params.put(vp, e.getValue());
                i++;
            }
        }
        idSql.append(" ORDER BY p.created_at DESC");

        @SuppressWarnings("unchecked")
        var idQuery = em.createNativeQuery(idSql.toString());
        params.forEach(idQuery::setParameter);

        List<Long> allIds = ((List<Object>) idQuery.getResultList())
                .stream().map(r -> ((Number) r).longValue()).toList();

        int total = allIds.size();
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), total);
        List<Long> pageIds = start >= total ? List.of() : allIds.subList(start, end);

        if (pageIds.isEmpty()) {
            return new PageImpl<>(List.of(), pageable, total);
        }

        // Load only the current page with variants eagerly fetched (no N+1).
        // Merge function (a, b) -> a handles the duplicate rows Hibernate produces from JOIN FETCH.
        Map<Long, Product> byId = em.createQuery(
                        "SELECT DISTINCT p FROM Product p LEFT JOIN FETCH p.variants WHERE p.id IN :ids",
                        Product.class)
                .setParameter("ids", pageIds)
                .getResultStream()
                .collect(Collectors.toMap(Product::getId, p -> p, (a, b) -> a));

        List<Product> pageProducts = pageIds.stream()
                .map(byId::get)
                .filter(p -> p != null)
                .toList();

        return new PageImpl<>(
                pageProducts.stream().map(ProductServiceImpl::toResponse).toList(), pageable, total);
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getFilterOptions(Long categoryId) {
        // Attribute keys come from product_variants, not products
        String keysSql = """
                SELECT DISTINCT jsonb_object_keys(pv.attributes)
                FROM product_variants pv
                JOIN products p ON p.id = pv.product_id
                WHERE p.category_id = :catId
                  AND p.status = 'ACTIVE'
                  AND pv.status = 'ACTIVE'
                ORDER BY 1
                """;
        List<String> keys = em.createNativeQuery(keysSql)
                .setParameter("catId", categoryId)
                .getResultList();

        List<Map<String, Object>> filters = new ArrayList<>();
        for (String key : keys) {
            String valSql = """
                    SELECT DISTINCT jsonb_extract_path_text(pv.attributes, :attrKey)
                    FROM product_variants pv
                    JOIN products p ON p.id = pv.product_id
                    WHERE p.category_id = :catId
                      AND p.status = 'ACTIVE'
                      AND pv.status = 'ACTIVE'
                      AND jsonb_extract_path_text(pv.attributes, :attrKey) IS NOT NULL
                    ORDER BY 1
                    """;
            List<String> values = em.createNativeQuery(valSql)
                    .setParameter("catId", categoryId)
                    .setParameter("attrKey", key)
                    .getResultList()
                    .stream()
                    .filter(v -> v != null)
                    .toList();
            if (!values.isEmpty()) {
                Map<String, Object> filter = new LinkedHashMap<>();
                filter.put("key", key);
                filter.put("label", toLabelName(key));
                filter.put("values", values);
                filters.add(filter);
            }
        }
        return filters;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Page<ProductResponse> adminSearch(String keyword, Long categoryId, String status, Pageable pageable) {
        StringBuilder jpql = new StringBuilder("SELECT p FROM Product p WHERE p.status <> :deleted");
        Map<String, Object> params = new HashMap<>();
        params.put("deleted", ProductStatus.DELETED);

        if (status != null && !status.isBlank()) {
            try {
                params.put("status", ProductStatus.valueOf(status));
                jpql.append(" AND p.status = :status");
            } catch (IllegalArgumentException ignored) {}
        }

        if (keyword != null && !keyword.isBlank()) {
            jpql.append(" AND (LOWER(p.name) LIKE :kw OR LOWER(p.brand) LIKE :kw)");
            params.put("kw", "%" + keyword.toLowerCase() + "%");
        }

        if (categoryId != null) {
            jpql.append(" AND p.category.id = :catId");
            params.put("catId", categoryId);
        }
        
        jpql.append(" ORDER BY p.createdAt DESC");

        var query = em.createQuery(jpql.toString(), Product.class);
        params.forEach(query::setParameter);

        String countJpql = jpql.toString()
                .replace("SELECT p FROM", "SELECT COUNT(p) FROM")
                .replace(" ORDER BY p.createdAt DESC", "");
        var countQuery = em.createQuery(countJpql, Long.class);
        params.forEach(countQuery::setParameter);
        long total = countQuery.getSingleResult();

        query.setFirstResult((int) pageable.getOffset());
        query.setMaxResults(pageable.getPageSize());

        List<Long> pageIds = query.getResultList().stream().map(Product::getId).toList();
        return mapIdListToResponses(pageIds, pageable, total);
    }

    @Override
    @Transactional
    public ProductResponse create(ProductRequest request) {
        Product product = new Product();
        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setBrand(request.getBrand());
        product.setCategory(resolveCategory(request.getCategoryId()));
        applyImages(product, request.getImageUrls());
        Product saved = productRepository.save(product);
        eventPublisher.publishProductCreated(new ProductCreatedEvent(saved.getId(), saved.getName()));
        productIndexService.index(saved);
        return toResponse(saved);
    }

    @Override
    @Transactional
    public ProductResponse update(Long id, ProductRequest request) {
        Product product = findActive(id);
        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setBrand(request.getBrand());
        product.setCategory(resolveCategory(request.getCategoryId()));
        if (request.getImageUrls() != null) {
            applyImages(product, request.getImageUrls());
        }
        Product saved = productRepository.save(product);
        productIndexService.index(saved);
        return toResponse(saved);
    }

    @Override
    @Transactional
    public ProductResponse uploadImages(Long id, List<MultipartFile> files) {
        Product product = findActive(id);
        List<String> existing = new ArrayList<>(product.getImageUrls());
        if (existing.size() >= 3) {
            throw new IllegalStateException("Maksimum 3 resim yüklenebilir.");
        }
        for (MultipartFile file : files) {
            if (existing.size() >= 3) break;
            existing.add(minioService.upload(file));
        }
        product.setImageUrls(existing);
        Product saved = productRepository.save(product);
        productIndexService.index(saved);
        return toResponse(saved);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Product product = findActive(id);
        product.setStatus(ProductStatus.DELETED);
        productRepository.save(product);
        productIndexService.delete(id);
    }

    // -- helpers -------------------------------------------------------------

    private void applyImages(Product product, List<String> imageUrls) {
        if (imageUrls != null && !imageUrls.isEmpty()) {
            List<String> urls = imageUrls.stream().limit(3).collect(Collectors.toList());
            product.setImageUrls(urls);
        }
    }

    private Product findActive(Long id) {
        return productRepository.findByIdAndStatusNot(id, ProductStatus.DELETED)
                .orElseThrow(() -> new ProductNotFoundException(id));
    }

    private Category resolveCategory(Long categoryId) {
        if (categoryId == null) return null;
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> new CategoryNotFoundException(categoryId));
    }

    private static String toLabelName(String key) {
        if (key == null || key.isBlank()) return key;
        return key.substring(0, 1).toUpperCase() + key.substring(1);
    }

    private static final Set<String> RESERVED_PARAMS = Set.of(
            "keyword", "categoryId", "page", "size", "sort"
    );

    private static Map<String, String> stripReservedParams(Map<String, String> params) {
        if (params == null || params.isEmpty()) return null;
        Map<String, String> filtered = new HashMap<>(params);
        RESERVED_PARAMS.forEach(filtered::remove);
        return filtered.isEmpty() ? null : filtered;
    }

    private Page<ProductResponse> mapIdPageToResponses(Page<Long> idPage, Pageable pageable) {
        if (idPage.isEmpty()) {
            return new PageImpl<>(List.of(), pageable, idPage.getTotalElements());
        }
        return mapIdListToResponses(idPage.getContent(), pageable, idPage.getTotalElements());
    }

    private Page<ProductResponse> mapIdListToResponses(List<Long> ids, Pageable pageable, long total) {
        if (ids == null || ids.isEmpty()) {
            return new PageImpl<>(List.of(), pageable, total);
        }

        List<Product> loaded = productRepository.findAllByIdInWithCategoryAndVariants(ids);
        Map<Long, Product> byId = new HashMap<>();
        for (Product p : loaded) {
            byId.put(p.getId(), p);
        }

        List<ProductResponse> content = ids.stream()
                .map(byId::get)
                .filter(p -> p != null)
                .map(ProductServiceImpl::toResponse)
                .toList();

        return new PageImpl<>(content, pageable, total);
    }

    public static ProductResponse toResponse(Product p) {
        ProductResponse res = new ProductResponse();
        res.setId(p.getId());
        res.setName(p.getName());
        res.setDescription(p.getDescription());
        res.setBrand(p.getBrand());
        List<String> urls = p.getImageUrls();
        res.setImageUrl(urls.isEmpty() ? null : urls.get(0));
        res.setImageUrls(urls);
        res.setInStock(p.getStatus() == ProductStatus.ACTIVE);
        res.setStatus(p.getStatus());
        res.setCreatedAt(p.getCreatedAt());
        res.setUpdatedAt(p.getUpdatedAt());
        if (p.getCategory() != null) {
            res.setCategory(CategoryServiceImpl.toResponse(p.getCategory()));
        }
        if (p.getVariants() != null && !p.getVariants().isEmpty()) {
            res.setVariants(p.getVariants().stream().map(VariantServiceImpl::toResponse).toList());

            // En düşük efektif fiyatı priceFrom olarak ata
            p.getVariants().stream()
                    .map(ProductVariant::getPrice)
                    .filter(price -> price != null)
                    .min(BigDecimal::compareTo)
                    .ifPresent(res::setPriceFrom);
        }
        return res;
    }
}
