package com.yabozkurt.n11bootcamp.ecommerce.order.infrastructure.client;

import com.yabozkurt.n11bootcamp.ecommerce.order.infrastructure.client.dto.ApiResponse;
import com.yabozkurt.n11bootcamp.ecommerce.order.infrastructure.client.dto.CouponValidationResponse;
import com.yabozkurt.n11bootcamp.ecommerce.order.infrastructure.client.dto.UseCouponRequest;
import com.yabozkurt.n11bootcamp.ecommerce.order.infrastructure.client.dto.ValidateCouponRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "coupon-service")
public interface CouponClient {

    @PostMapping("/api/coupons/validate")
    ApiResponse<CouponValidationResponse> validate(
            @RequestHeader("X-User-Id") String userId,
            @RequestBody ValidateCouponRequest request);

    @PostMapping("/api/coupons/use")
    ApiResponse<CouponValidationResponse> use(
            @RequestHeader("X-User-Id") String userId,
            @RequestBody UseCouponRequest request);
}
