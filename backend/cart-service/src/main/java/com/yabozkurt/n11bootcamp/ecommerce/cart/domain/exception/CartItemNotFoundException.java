package com.yabozkurt.n11bootcamp.ecommerce.cart.domain.exception;

public class CartItemNotFoundException extends RuntimeException {
    public CartItemNotFoundException(String itemId) {
        super("Sepet kalemi bulunamadı: " + itemId);
    }
}
