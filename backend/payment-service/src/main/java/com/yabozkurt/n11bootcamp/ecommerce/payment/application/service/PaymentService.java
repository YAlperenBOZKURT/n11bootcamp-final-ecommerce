package com.yabozkurt.n11bootcamp.ecommerce.payment.application.service;

import com.yabozkurt.n11bootcamp.ecommerce.payment.presentation.dto.request.PaymentCheckoutRequest;
import com.yabozkurt.n11bootcamp.ecommerce.payment.presentation.dto.request.RefundRequest;
import com.yabozkurt.n11bootcamp.ecommerce.payment.presentation.dto.response.PaymentResponse;

public interface PaymentService {
    PaymentResponse checkout(PaymentCheckoutRequest request);
    PaymentResponse refund(RefundRequest request);
    PaymentResponse getByOrderId(String orderId);
}
