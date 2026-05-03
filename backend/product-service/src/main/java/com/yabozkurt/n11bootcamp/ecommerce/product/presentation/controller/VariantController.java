package com.yabozkurt.n11bootcamp.ecommerce.product.presentation.controller;

import com.yabozkurt.n11bootcamp.ecommerce.product.application.service.VariantService;
import com.yabozkurt.n11bootcamp.ecommerce.product.presentation.dto.request.VariantRequest;
import com.yabozkurt.n11bootcamp.ecommerce.product.presentation.dto.response.ApiResponse;
import com.yabozkurt.n11bootcamp.ecommerce.product.presentation.dto.response.VariantResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/products/{productId}/variants")
public class VariantController {

    private final VariantService variantService;

    public VariantController(VariantService variantService) {
        this.variantService = variantService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<VariantResponse>>> getAll(@PathVariable Long productId) {
        return ResponseEntity.ok(ApiResponse.ok(variantService.getByProductId(productId)));
    }

    @GetMapping("/{variantId}")
    public ResponseEntity<ApiResponse<VariantResponse>> getById(@PathVariable Long productId,
                                                                 @PathVariable Long variantId) {
        return ResponseEntity.ok(ApiResponse.ok(variantService.getById(productId, variantId)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<VariantResponse>> create(@PathVariable Long productId,
                                                                @Valid @RequestBody VariantRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(variantService.create(productId, request)));
    }

    @PutMapping("/{variantId}")
    public ResponseEntity<ApiResponse<VariantResponse>> update(@PathVariable Long productId,
                                                                @PathVariable Long variantId,
                                                                @Valid @RequestBody VariantRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(variantService.update(productId, variantId, request)));
    }

    @DeleteMapping("/{variantId}")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable Long productId,
                                                     @PathVariable Long variantId) {
        variantService.delete(productId, variantId);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }
}
