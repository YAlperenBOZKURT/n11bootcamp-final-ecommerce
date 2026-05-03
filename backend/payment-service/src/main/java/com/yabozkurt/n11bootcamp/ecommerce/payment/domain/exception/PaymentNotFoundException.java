package com.yabozkurt.n11bootcamp.ecommerce.payment.domain.exception;

public class PaymentNotFoundException extends RuntimeException {
    public PaymentNotFoundException(String orderId) {
        super("Ödeme kaydı bulunamadı: " + orderId);
    }
}
