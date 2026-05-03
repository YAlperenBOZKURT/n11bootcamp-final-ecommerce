package com.yabozkurt.n11bootcamp.ecommerce.user.domain.exception;

public class InvalidTokenException extends RuntimeException {

    public InvalidTokenException(String message) {
        super(message);
    }
}
