package com.yabozkurt.n11bootcamp.coupon.domain.exception;

public class CouponValidationException extends RuntimeException {
    public CouponValidationException(String message) {
        super(message);
    }
}
