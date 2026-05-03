package com.yabozkurt.n11bootcamp.ecommerce.product.application.service;

import com.yabozkurt.n11bootcamp.ecommerce.product.presentation.dto.request.ProductRequest;
import com.yabozkurt.n11bootcamp.ecommerce.product.presentation.dto.response.ProductResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

public interface ProductService {
    ProductResponse getById(Long id);
    Page<ProductResponse> getAll(Pageable pageable);
    Page<ProductResponse> getByCategory(Long categoryId, Pageable pageable);
    Page<ProductResponse> search(String keyword, Long categoryId, Map<String, String> attributes, Pageable pageable);
    List<Map<String, Object>> getFilterOptions(Long categoryId);
    // Admin search with status filter (ACTIVE, PASSIVE) 
    Page<ProductResponse> adminSearch(String keyword, Long categoryId, String status, Pageable pageable);
    ProductResponse create(ProductRequest request);
    ProductResponse update(Long id, ProductRequest request);
    // Upload one or more images in a single multipart request
    ProductResponse uploadImages(Long id, List<MultipartFile> files);
    void delete(Long id);
}
