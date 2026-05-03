package com.yabozkurt.n11bootcamp.ecommerce.cart.domain.exception;

public class CartValidationException extends RuntimeException {
    public CartValidationException(String message) {
        super(message);
    }
}
