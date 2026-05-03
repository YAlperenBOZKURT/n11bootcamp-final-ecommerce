package com.yabozkurt.n11bootcamp.ecommerce.product.application.service;

import com.yabozkurt.n11bootcamp.ecommerce.product.presentation.dto.request.VariantRequest;
import com.yabozkurt.n11bootcamp.ecommerce.product.presentation.dto.response.VariantResponse;

import java.util.List;

public interface VariantService {

    List<VariantResponse> getByProductId(Long productId);

    VariantResponse getById(Long productId, Long variantId);

    VariantResponse create(Long productId, VariantRequest request);

    VariantResponse update(Long productId, Long variantId, VariantRequest request);

    void delete(Long productId, Long variantId);
}
