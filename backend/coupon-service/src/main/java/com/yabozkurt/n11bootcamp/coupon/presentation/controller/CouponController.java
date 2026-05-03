package com.yabozkurt.n11bootcamp.coupon.presentation.controller;

import com.yabozkurt.n11bootcamp.coupon.application.service.CouponService;
import com.yabozkurt.n11bootcamp.coupon.domain.exception.CouponValidationException;
import com.yabozkurt.n11bootcamp.coupon.domain.model.enums.CouponType;
import com.yabozkurt.n11bootcamp.coupon.presentation.dto.request.*;
import com.yabozkurt.n11bootcamp.coupon.presentation.dto.response.ApiResponse;
import com.yabozkurt.n11bootcamp.coupon.presentation.dto.response.CouponResponse;
import com.yabozkurt.n11bootcamp.coupon.presentation.dto.response.CouponValidationResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/coupons")
@Tag(name = "Coupons", description = "Coupon management, claiming, validation, and usage")
@SecurityRequirement(name = "cookieAuth")
public class CouponController {

    private static final String ADMIN_ROLE = "ADMIN";
    private final CouponService couponService;

    public CouponController(CouponService couponService) {
        this.couponService = couponService;
    }

    @PostMapping
    @Operation(summary = "Create coupon (ADMIN)")
    public ResponseEntity<ApiResponse<CouponResponse>> create(@RequestHeader(name = "X-User-Role", required = false) String role,
                                                              @Valid @RequestBody CreateCouponRequest request) {
        requireAdmin(role);
        return ResponseEntity.ok(ApiResponse.ok(couponService.create(request)));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update coupon (ADMIN)")
    public ResponseEntity<ApiResponse<CouponResponse>> update(@RequestHeader(name = "X-User-Role", required = false) String role,
                                                              @PathVariable Long id,
                                                              @Valid @RequestBody UpdateCouponRequest request) {
        requireAdmin(role);
        return ResponseEntity.ok(ApiResponse.ok(couponService.update(id, request)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get coupon by ID")
    public ResponseEntity<ApiResponse<CouponResponse>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(couponService.getById(id)));
    }

    @GetMapping
    @Operation(summary = "List coupons")
    public ResponseEntity<ApiResponse<Page<CouponResponse>>> getAll(
            @RequestParam(required = false) Boolean active,
            @RequestParam(required = false) CouponType type,
            @RequestParam(required = false) String code,
            @PageableDefault(size = 20) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok(couponService.getAll(active, type, code, pageable)));
    }

    @GetMapping("/my")
    @Operation(summary = "List my claimed coupons")
    public ResponseEntity<ApiResponse<List<CouponResponse>>> getMyCoupons(
            @RequestHeader(name = "X-User-Id", required = false) String userIdHeader) {
        return ResponseEntity.ok(ApiResponse.ok(couponService.getMyCoupons(parseUserId(userIdHeader))));
    }

    @PatchMapping("/{id}/active")
    @Operation(summary = "Set coupon active/passive (ADMIN)")
    public ResponseEntity<ApiResponse<CouponResponse>> setActive(@RequestHeader(name = "X-User-Role", required = false) String role,
                                                                 @PathVariable Long id,
                                                                 @RequestParam boolean value) {
        requireAdmin(role);
        return ResponseEntity.ok(ApiResponse.ok(couponService.setActive(id, value)));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete (passivate) coupon (ADMIN)")
    public ResponseEntity<ApiResponse<Void>> delete(@RequestHeader(name = "X-User-Role", required = false) String role,
                                                    @PathVariable Long id) {
        requireAdmin(role);
        couponService.delete(id);
        return ResponseEntity.ok(ApiResponse.ok("Kupon pasife alındı", null));
    }

    @PostMapping("/claim")
    @Operation(summary = "Claim coupon to current user")
    public ResponseEntity<ApiResponse<Void>> claim(@RequestHeader(name = "X-User-Id", required = false) String userIdHeader,
                                                   @Valid @RequestBody ClaimCouponRequest request) {
        couponService.claim(parseUserId(userIdHeader), request);
        return ResponseEntity.ok(ApiResponse.ok("Kupon hesaba eklendi", null));
    }

    @PostMapping("/validate")
    @Operation(summary = "Validate coupon for checkout")
    public ResponseEntity<ApiResponse<CouponValidationResponse>> validate(@RequestHeader(name = "X-User-Id", required = false) String userIdHeader,
                                                                          @Valid @RequestBody ValidateCouponRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(couponService.validate(parseUserId(userIdHeader), request)));
    }

    @PostMapping("/use")
    @Operation(summary = "Consume coupon after successful checkout")
    public ResponseEntity<ApiResponse<CouponValidationResponse>> use(@RequestHeader(name = "X-User-Id", required = false) String userIdHeader,
                                                                     @Valid @RequestBody UseCouponRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(couponService.use(parseUserId(userIdHeader), request)));
    }

    private void requireAdmin(String role) {
        if (!ADMIN_ROLE.equals(role)) {
            throw new CouponValidationException("Bu işlem için ADMIN rolü gerekli");
        }
    }

    private Long parseUserId(String userIdHeader) {
        if (userIdHeader == null || userIdHeader.isBlank()) {
            throw new CouponValidationException("X-User-Id header zorunlu");
        }
        try {
            return Long.valueOf(userIdHeader);
        } catch (NumberFormatException ex) {
            throw new CouponValidationException("X-User-Id geçersiz");
        }
    }
}
