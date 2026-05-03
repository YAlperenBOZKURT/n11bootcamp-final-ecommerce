package com.yabozkurt.n11bootcamp.ecommerce.user.domain.exception;

public class AddressNotFoundException extends RuntimeException {

    public AddressNotFoundException(Long id) {
        super("Address not found: " + id);
    }
}
