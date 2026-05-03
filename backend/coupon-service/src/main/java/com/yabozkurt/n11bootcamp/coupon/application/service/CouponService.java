package com.yabozkurt.n11bootcamp.coupon.application.service;

import com.yabozkurt.n11bootcamp.coupon.domain.model.enums.CouponType;
import com.yabozkurt.n11bootcamp.coupon.presentation.dto.request.*;
import com.yabozkurt.n11bootcamp.coupon.presentation.dto.response.CouponResponse;
import com.yabozkurt.n11bootcamp.coupon.presentation.dto.response.CouponValidationResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface CouponService {
    CouponResponse create(CreateCouponRequest request);
    CouponResponse update(Long id, UpdateCouponRequest request);
    CouponResponse getById(Long id);
    Page<CouponResponse> getAll(Boolean active, CouponType type, String code, Pageable pageable);
    List<CouponResponse> getMyCoupons(Long userId);
    CouponResponse setActive(Long id, boolean active);
    void delete(Long id);
    void claim(Long userId, ClaimCouponRequest request);
    CouponValidationResponse validate(Long userId, ValidateCouponRequest request);
    CouponValidationResponse use(Long userId, UseCouponRequest request);
}
