package com.yabozkurt.n11bootcamp.ecommerce.order.presentation.controller;

import com.yabozkurt.n11bootcamp.ecommerce.order.application.service.OrderService;
import com.yabozkurt.n11bootcamp.ecommerce.order.domain.exception.OrderValidationException;
import com.yabozkurt.n11bootcamp.ecommerce.order.presentation.dto.request.OrderRequest;
import com.yabozkurt.n11bootcamp.ecommerce.order.presentation.dto.response.ApiResponse;
import com.yabozkurt.n11bootcamp.ecommerce.order.presentation.dto.response.OrderResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
@Tag(name = "Orders", description = "Order placement, query, and cancellation operations")
@SecurityRequirement(name = "cookieAuth")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    @Operation(summary = "Place order", description = "Creates a new order and starts checkout saga")
    public ResponseEntity<ApiResponse<OrderResponse>> placeOrder(
            @RequestHeader(name = "X-User-Id", required = false) String userIdHeader,
            @RequestHeader(name = "X-User-Email", required = false) String userEmail,
            @Valid @RequestBody OrderRequest request) {
        Long userId = parseUserId(userIdHeader);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(orderService.placeOrder(userId, userEmail, request)));
    }

    @GetMapping("/{orderNumber}")
    @Operation(summary = "Get order by order number")
    public ResponseEntity<ApiResponse<OrderResponse>> getByOrderNumber(
            @RequestHeader(name = "X-User-Id", required = false) String userIdHeader,
            @RequestHeader(name = "X-User-Role", required = false) String role,
            @PathVariable String orderNumber) {
        Long userId = parseUserId(userIdHeader);
        return ResponseEntity.ok(ApiResponse.ok(orderService.getByOrderNumber(userId, role, orderNumber)));
    }

    @GetMapping("/my")
    @Operation(summary = "Get my orders")
    public ResponseEntity<ApiResponse<List<OrderResponse>>> getMyOrders(
            @RequestHeader(name = "X-User-Id", required = false) String userIdHeader) {
        Long userId = parseUserId(userIdHeader);
        return ResponseEntity.ok(ApiResponse.ok(orderService.getOrdersByUser(userId)));
    }

    @GetMapping("/admin")
    @Operation(summary = "List all orders (admin)")
    public ResponseEntity<ApiResponse<Page<OrderResponse>>> getAllOrders(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok(orderService.getAllOrders(pageable)));
    }

    @PostMapping("/{orderNumber}/cancel")
    @Operation(summary = "Cancel order")
    public ResponseEntity<ApiResponse<OrderResponse>> cancelOrder(
            @RequestHeader(name = "X-User-Id", required = false) String userIdHeader,
            @PathVariable String orderNumber) {
        Long userId = parseUserId(userIdHeader);
        return ResponseEntity.ok(ApiResponse.ok(orderService.cancelOrder(userId, orderNumber)));
    }

    private Long parseUserId(String userIdHeader) {
        if (userIdHeader == null || userIdHeader.isBlank()) {
            throw new OrderValidationException("X-User-Id header zorunlu");
        }
        try {
            return Long.valueOf(userIdHeader);
        } catch (NumberFormatException ex) {
            throw new OrderValidationException("X-User-Id geçersiz");
        }
    }
}
