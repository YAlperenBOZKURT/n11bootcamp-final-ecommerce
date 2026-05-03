package com.yabozkurt.n11bootcamp.ecommerce.order.domain.exception;

public class OrderNotFoundException extends RuntimeException {
    public OrderNotFoundException(String orderNumber) {
        super("Order not found: " + orderNumber);
    }
    public OrderNotFoundException(Long id) {
        super("Order not found with id: " + id);
    }
}
