package com.yabozkurt.n11bootcamp.ecommerce.order.domain.exception;

public class OrderValidationException extends RuntimeException {
    public OrderValidationException(String message) {
        super(message);
    }
}
