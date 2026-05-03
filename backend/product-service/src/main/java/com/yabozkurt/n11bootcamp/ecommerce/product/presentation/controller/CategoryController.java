package com.yabozkurt.n11bootcamp.ecommerce.product.presentation.controller;

import com.yabozkurt.n11bootcamp.ecommerce.product.application.service.CategoryService;
import com.yabozkurt.n11bootcamp.ecommerce.product.presentation.dto.request.CategoryRequest;
import com.yabozkurt.n11bootcamp.ecommerce.product.presentation.dto.response.ApiResponse;
import com.yabozkurt.n11bootcamp.ecommerce.product.presentation.dto.response.CategoryResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/products/categories")
public class CategoryController {

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<CategoryResponse>>> getAll() {
        return ResponseEntity.ok(ApiResponse.ok(categoryService.getAll()));
    }

    @GetMapping("/roots")
    public ResponseEntity<ApiResponse<List<CategoryResponse>>> getRoots() {
        return ResponseEntity.ok(ApiResponse.ok(categoryService.getRootCategories()));
    }

    @GetMapping("/{id}/children")
    public ResponseEntity<ApiResponse<List<CategoryResponse>>> getChildren(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(categoryService.getChildren(id)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CategoryResponse>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(categoryService.getById(id)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<CategoryResponse>> create(@Valid @RequestBody CategoryRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(categoryService.create(request)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<CategoryResponse>> update(@PathVariable Long id,
                                                                @Valid @RequestBody CategoryRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(categoryService.update(id, request)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long id) {
        categoryService.delete(id);
        return ResponseEntity.ok(ApiResponse.ok("Kategori silindi", null));
    }
}
