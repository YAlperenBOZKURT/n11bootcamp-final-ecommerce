package com.yabozkurt.n11bootcamp.ecommerce.order.infrastructure.client;

import com.yabozkurt.n11bootcamp.ecommerce.order.infrastructure.client.dto.ApiResponse;
import com.yabozkurt.n11bootcamp.ecommerce.order.infrastructure.client.dto.StockReserveRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "stock-service")
public interface StockClient {

    @PostMapping("/api/stocks/variants/{variantId}/reserve")
    ApiResponse<Object> reserveVariant(@PathVariable Long variantId, @RequestBody StockReserveRequest request);

    @PostMapping("/api/stocks/variants/{variantId}/release")
    ApiResponse<Object> releaseVariant(@PathVariable Long variantId, @RequestBody StockReserveRequest request);

    @PostMapping("/api/stocks/variants/{variantId}/confirm")
    ApiResponse<Object> confirmVariant(@PathVariable Long variantId, @RequestBody StockReserveRequest request);
}
