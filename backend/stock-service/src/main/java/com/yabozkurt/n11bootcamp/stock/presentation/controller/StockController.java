package com.yabozkurt.n11bootcamp.stock.presentation.controller;

import com.yabozkurt.n11bootcamp.stock.application.service.StockService;
import com.yabozkurt.n11bootcamp.stock.presentation.dto.request.ReserveRequest;
import com.yabozkurt.n11bootcamp.stock.presentation.dto.request.StockUpdateRequest;
import com.yabozkurt.n11bootcamp.stock.presentation.dto.response.ApiResponse;
import com.yabozkurt.n11bootcamp.stock.presentation.dto.response.StockMovementResponse;
import com.yabozkurt.n11bootcamp.stock.presentation.dto.response.StockResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/stocks")
@Tag(name = "Stocks", description = "Stock reserve/release/confirm and variant stock operations")
@SecurityRequirement(name = "cookieAuth")
public class StockController {

    private final StockService stockService;

    public StockController(StockService stockService) {
        this.stockService = stockService;
    }

    @GetMapping("/{productId}/variants")
    @Operation(summary = "List all variant stocks for a product")
    public ResponseEntity<ApiResponse<List<StockResponse>>> getAllVariantStocks(@PathVariable Long productId) {
        return ResponseEntity.ok(ApiResponse.ok(stockService.getAllVariantStocks(productId)));
    }


    @GetMapping("/variants/{variantId}")
    @Operation(summary = "Get stock by variant ID")
    public ResponseEntity<ApiResponse<StockResponse>> getByVariantId(@PathVariable Long variantId) {
        return ResponseEntity.ok(ApiResponse.ok(stockService.getByVariantId(variantId)));
    }

    // Initialize stock row for variant (called by admin panel when variant is created)
    @PostMapping("/{productId}/variants/{variantId}/init")
    @Operation(summary = "Initialize stock row for variant")
    public ResponseEntity<ApiResponse<Void>> initVariantStock(@PathVariable Long productId,
                                                               @PathVariable Long variantId) {
        stockService.initVariantStock(productId, variantId);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    @PostMapping("/variants/{variantId}/add")
    @Operation(summary = "Add stock by variant ID")
    public ResponseEntity<ApiResponse<StockResponse>> addVariantStock(@PathVariable Long variantId,
                                                                        @Valid @RequestBody StockUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(stockService.addStockByVariant(variantId, request)));
    }

    @PostMapping("/variants/{variantId}/reserve")
    @Operation(summary = "Reserve stock by variant ID")
    public ResponseEntity<ApiResponse<StockResponse>> reserveVariant(@PathVariable Long variantId,
                                                                       @Valid @RequestBody ReserveRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(stockService.reserveByVariant(variantId, request)));
    }

    @PostMapping("/variants/{variantId}/release")
    @Operation(summary = "Release reserved stock by variant ID")
    public ResponseEntity<ApiResponse<StockResponse>> releaseVariant(@PathVariable Long variantId,
                                                                       @Valid @RequestBody ReserveRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(stockService.releaseByVariant(variantId, request)));
    }

    @PostMapping("/variants/{variantId}/confirm")
    @Operation(summary = "Confirm reserved stock by variant ID")
    public ResponseEntity<ApiResponse<StockResponse>> confirmVariant(@PathVariable Long variantId,
                                                                       @Valid @RequestBody ReserveRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(stockService.confirmByVariant(variantId, request)));
    }

    @GetMapping("/variants/{variantId}/movements")
    @Operation(summary = "Get stock movements by variant ID")
    public ResponseEntity<ApiResponse<List<StockMovementResponse>>> getVariantMovements(@PathVariable Long variantId) {
        return ResponseEntity.ok(ApiResponse.ok(stockService.getMovementsByVariantId(variantId)));
    }
}
