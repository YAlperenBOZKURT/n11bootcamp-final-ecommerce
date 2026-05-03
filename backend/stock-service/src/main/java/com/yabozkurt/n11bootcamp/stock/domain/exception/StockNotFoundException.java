package com.yabozkurt.n11bootcamp.stock.domain.exception;

public class StockNotFoundException extends RuntimeException {
    public StockNotFoundException(Long productId) {
        super("Stock not found for product: " + productId);
    }
}
