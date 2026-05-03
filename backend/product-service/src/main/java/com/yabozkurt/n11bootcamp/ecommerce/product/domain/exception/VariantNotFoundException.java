package com.yabozkurt.n11bootcamp.ecommerce.product.domain.exception;

public class VariantNotFoundException extends RuntimeException {
    public VariantNotFoundException(Long id) {
        super("Variant not found with id: " + id);
    }
}
