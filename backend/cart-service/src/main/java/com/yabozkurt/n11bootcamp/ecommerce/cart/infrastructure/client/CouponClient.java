package com.yabozkurt.n11bootcamp.ecommerce.cart.infrastructure.client;

import com.yabozkurt.n11bootcamp.ecommerce.cart.infrastructure.client.dto.ApiResponse;
import com.yabozkurt.n11bootcamp.ecommerce.cart.infrastructure.client.dto.CouponValidateRequest;
import com.yabozkurt.n11bootcamp.ecommerce.cart.infrastructure.client.dto.CouponValidationData;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "coupon-service")
public interface CouponClient {

    @PostMapping("/api/coupons/validate")
    ApiResponse<CouponValidationData> validate(
            @RequestHeader("X-User-Id") String userId,
            @RequestBody CouponValidateRequest request
    );
}
