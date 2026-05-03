package com.yabozkurt.n11bootcamp.coupon.presentation.dto.request;

import com.yabozkurt.n11bootcamp.coupon.domain.model.enums.CouponType;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class CreateCouponRequest {

    @NotBlank
    @Size(max = 64)
    private String code;

    @NotNull
    private CouponType couponType;

    @NotNull
    @DecimalMin(value = "0.01")
    @DecimalMax(value = "100.00")
    private BigDecimal discountRate;

    @NotNull
    @DecimalMin(value = "0.00")
    private BigDecimal minOrderAmount;

    @NotNull
    private LocalDateTime startAt;

    @NotNull
    private LocalDateTime endAt;

    @Min(1)
    private Integer perUserLimit;

    private Boolean active = true;

    public String getCode() {
        return code;
    }

    public CouponType getCouponType() {
        return couponType;
    }

    public BigDecimal getDiscountRate() {
        return discountRate;
    }

    public BigDecimal getMinOrderAmount() {
        return minOrderAmount;
    }

    public LocalDateTime getStartAt() {
        return startAt;
    }

    public LocalDateTime getEndAt() {
        return endAt;
    }

    public Integer getPerUserLimit() {
        return perUserLimit;
    }

    public Boolean getActive() {
        return active;
    }
}
