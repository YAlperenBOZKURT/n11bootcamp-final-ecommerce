package com.yabozkurt.n11bootcamp.ecommerce.payment.presentation.controller;

import com.yabozkurt.n11bootcamp.ecommerce.payment.application.service.PaymentService;
import com.yabozkurt.n11bootcamp.ecommerce.payment.presentation.dto.request.PaymentCheckoutRequest;
import com.yabozkurt.n11bootcamp.ecommerce.payment.presentation.dto.request.RefundRequest;
import com.yabozkurt.n11bootcamp.ecommerce.payment.presentation.dto.response.ApiResponse;
import com.yabozkurt.n11bootcamp.ecommerce.payment.presentation.dto.response.PaymentResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments")
@Tag(name = "Payments", description = "Payment checkout, refund, and query operations")
@SecurityRequirement(name = "cookieAuth")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/checkout")
    @Operation(summary = "Checkout payment")
    public ResponseEntity<ApiResponse<PaymentResponse>> checkout(@Valid @RequestBody PaymentCheckoutRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(paymentService.checkout(request)));
    }

    @PostMapping("/refund")
    @Operation(summary = "Refund payment")
    public ResponseEntity<ApiResponse<PaymentResponse>> refund(@Valid @RequestBody RefundRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(paymentService.refund(request)));
    }

    @GetMapping("/{orderId}")
    @Operation(summary = "Get payment by order ID")
    public ResponseEntity<ApiResponse<PaymentResponse>> getByOrderId(@PathVariable String orderId) {
        return ResponseEntity.ok(ApiResponse.ok(paymentService.getByOrderId(orderId)));
    }
}
