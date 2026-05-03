package com.yabozkurt.n11bootcamp.ecommerce.product.domain.exception;

public class CategoryNotFoundException extends RuntimeException {
    public CategoryNotFoundException(Long id) {
        super("Category not found with id: " + id);
    }
}
