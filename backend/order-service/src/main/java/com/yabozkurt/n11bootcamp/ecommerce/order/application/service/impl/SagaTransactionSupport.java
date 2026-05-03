package com.yabozkurt.n11bootcamp.ecommerce.order.application.service.impl;

import com.yabozkurt.n11bootcamp.ecommerce.order.domain.model.Order;
import com.yabozkurt.n11bootcamp.ecommerce.order.domain.model.enums.OrderStatus;
import com.yabozkurt.n11bootcamp.ecommerce.order.domain.repository.OrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

// Small helper for order saga DB writes.
// Each method uses REQUIRES_NEW, so critical state changes are committed immediately.
// This keeps order state durable even if later saga steps fail.
// Kept as a separate Spring bean so transactional proxies work correctly.
@Service
public class SagaTransactionSupport {

    private final OrderRepository orderRepository;

    public SagaTransactionSupport(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    // Save the first PENDING order record right away.
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Order savePending(Order order) {
        return orderRepository.save(order);
    }

    // Mark order as FAILED and commit immediately (used in compensation paths).
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Order markFailed(Order order, String reason) {
        order.setStatus(OrderStatus.FAILED);
        order.setFailReason(cap(reason, 500));
        order.setFinalAmount(order.getTotalAmount());
        return orderRepository.save(order);
    }

    // Mark order as CONFIRMED after successful payment capture.
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Order markConfirmed(Order order,
                               BigDecimal discountAmount,
                               BigDecimal finalAmount,
                               String couponCode) {
        order.setStatus(OrderStatus.CONFIRMED);
        order.setDiscountAmount(discountAmount != null ? discountAmount : BigDecimal.ZERO);
        order.setFinalAmount(finalAmount);
        if (couponCode != null && !couponCode.isBlank()) {
            order.setCouponCode(couponCode);
        }
        return orderRepository.save(order);
    }

    // Mark order as CANCELLED and commit immediately.
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Order markCancelled(Order order, String reason) {
        order.setStatus(OrderStatus.CANCELLED);
        order.setFailReason(cap(reason, 500));
        return orderRepository.save(order);
    }

    private static String cap(String s, int max) {
        if (s == null || s.length() <= max) return s;
        return s.substring(0, max);
    }
}
