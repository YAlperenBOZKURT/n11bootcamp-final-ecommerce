package com.yabozkurt.n11bootcamp.coupon.domain.exception;

public class CouponNotFoundException extends RuntimeException {
    public CouponNotFoundException(String code) {
        super("Kupon bulunamadı: " + code);
    }

    public CouponNotFoundException(Long id) {
        super("Kupon bulunamadı: " + id);
    }
}
