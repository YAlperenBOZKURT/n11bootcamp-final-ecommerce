package com.yabozkurt.n11bootcamp.ecommerce.order.application.service;

import com.yabozkurt.n11bootcamp.ecommerce.order.presentation.dto.request.OrderRequest;
import com.yabozkurt.n11bootcamp.ecommerce.order.presentation.dto.response.OrderResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface OrderService {
    OrderResponse placeOrder(Long userId, String userEmail, OrderRequest request);
    OrderResponse getByOrderNumber(Long userId, String role, String orderNumber);
    List<OrderResponse> getOrdersByUser(Long userId);
    OrderResponse cancelOrder(Long userId, String orderNumber);
    Page<OrderResponse> getAllOrders(Pageable pageable);
}
