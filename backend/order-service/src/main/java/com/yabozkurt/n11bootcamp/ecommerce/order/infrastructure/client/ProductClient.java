package com.yabozkurt.n11bootcamp.ecommerce.order.infrastructure.client;

import com.yabozkurt.n11bootcamp.ecommerce.order.infrastructure.client.dto.ApiResponse;
import com.yabozkurt.n11bootcamp.ecommerce.order.infrastructure.client.dto.ProductClientResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "product-service")
public interface ProductClient {

    @GetMapping("/api/products/{id}")
    ApiResponse<ProductClientResponse> getById(@PathVariable Long id);
}
