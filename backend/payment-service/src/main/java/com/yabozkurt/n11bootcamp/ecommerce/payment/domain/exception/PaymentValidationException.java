package com.yabozkurt.n11bootcamp.ecommerce.payment.domain.exception;

public class PaymentValidationException extends RuntimeException {
    public PaymentValidationException(String message) {
        super(message);
    }
}
