package com.yabozkurt.n11bootcamp.stock.application.service;

import com.yabozkurt.n11bootcamp.stock.presentation.dto.request.ReserveRequest;
import com.yabozkurt.n11bootcamp.stock.presentation.dto.request.StockUpdateRequest;
import com.yabozkurt.n11bootcamp.stock.presentation.dto.response.StockMovementResponse;
import com.yabozkurt.n11bootcamp.stock.presentation.dto.response.StockResponse;

import java.util.List;

public interface StockService {
    void initVariantStock(Long productId, Long variantId);

    StockResponse getByVariantId(Long variantId);

    StockResponse addStockByVariant(Long variantId, StockUpdateRequest request);

    StockResponse reserveByVariant(Long variantId, ReserveRequest request);

    StockResponse releaseByVariant(Long variantId, ReserveRequest request);

    StockResponse confirmByVariant(Long variantId, ReserveRequest request);

    List<StockMovementResponse> getMovementsByVariantId(Long variantId);

    List<StockResponse> getAllVariantStocks(Long productId);
}
