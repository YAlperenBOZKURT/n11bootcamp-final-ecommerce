package com.yabozkurt.n11bootcamp.ecommerce.cart.infrastructure.client.dto;

public class ApiResponse<T> {
    private boolean success;
    private String message;
    private T data;

    public boolean isSuccess() { return success; }
    public String getMessage() { return message; }
    public T getData() { return data; }
}
