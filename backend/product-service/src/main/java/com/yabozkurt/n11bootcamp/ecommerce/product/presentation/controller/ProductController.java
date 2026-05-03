package com.yabozkurt.n11bootcamp.ecommerce.product.presentation.controller;

import com.yabozkurt.n11bootcamp.ecommerce.product.application.service.ProductService;
import com.yabozkurt.n11bootcamp.ecommerce.product.presentation.dto.request.ProductRequest;
import com.yabozkurt.n11bootcamp.ecommerce.product.presentation.dto.response.ApiResponse;
import com.yabozkurt.n11bootcamp.ecommerce.product.presentation.dto.response.ProductResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Page<ProductResponse>>> getAll(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok(productService.getAll(pageable)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductResponse>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(productService.getById(id)));
    }

    @GetMapping("/category/{categoryId}")
    public ResponseEntity<ApiResponse<Page<ProductResponse>>> getByCategory(
            @PathVariable Long categoryId,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok(productService.getByCategory(categoryId, pageable)));
    }

    @GetMapping("/search")
    public ResponseEntity<ApiResponse<Page<ProductResponse>>> search(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Map<String, String> attributes,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok(productService.search(keyword, categoryId, attributes, pageable)));
    }

    @GetMapping("/filters")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getFilters(
            @RequestParam Long categoryId) {
        return ResponseEntity.ok(ApiResponse.ok(productService.getFilterOptions(categoryId)));
    }

    // This is for admin panel to list products with optional status filter (ACTIVE,
    // PASSIVE, etc.). If status is empty/null, it returns all non-DELETED products.
    @GetMapping("/admin/search")
    public ResponseEntity<ApiResponse<Page<ProductResponse>>> adminSearch(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String status,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok(productService.adminSearch(keyword, categoryId, status, pageable)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ProductResponse>> create(@Valid @RequestBody ProductRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(productService.create(request)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductResponse>> update(@PathVariable Long id,
            @Valid @RequestBody ProductRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(productService.update(id, request)));
    }

    // Uploads one or more product images via multipart/form-data and returns the updated product
    @PostMapping(value = "/{id}/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<ProductResponse>> uploadImages(@PathVariable Long id,
            @RequestParam List<MultipartFile> files) {
        return ResponseEntity.ok(ApiResponse.ok(productService.uploadImages(id, files)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        productService.delete(id);
        return ResponseEntity.ok(ApiResponse.ok("Ürün silindi", null));
    }
}
