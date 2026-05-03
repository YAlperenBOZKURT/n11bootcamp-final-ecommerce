package com.yabozkurt.n11bootcamp.ecommerce.order.infrastructure.client;

import com.yabozkurt.n11bootcamp.ecommerce.order.infrastructure.client.dto.ApiResponse;
import com.yabozkurt.n11bootcamp.ecommerce.order.infrastructure.client.dto.PaymentCheckoutRequest;
import com.yabozkurt.n11bootcamp.ecommerce.order.infrastructure.client.dto.PaymentResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "payment-service")
public interface PaymentClient {

    @PostMapping("/api/payments/checkout")
    ApiResponse<PaymentResponse> checkout(@RequestBody PaymentCheckoutRequest request);
}
