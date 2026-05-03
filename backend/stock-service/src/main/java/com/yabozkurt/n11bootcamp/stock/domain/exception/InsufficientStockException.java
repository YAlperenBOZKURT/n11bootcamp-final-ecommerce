package com.yabozkurt.n11bootcamp.stock.domain.exception;

public class InsufficientStockException extends RuntimeException {
    public InsufficientStockException(Long productId, int requested, int available) {
        super("Insufficient stock for product " + productId + ": requested=" + requested + ", available=" + available);
    }
}
