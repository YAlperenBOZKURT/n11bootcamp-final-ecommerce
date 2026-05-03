package com.yabozkurt.n11bootcamp.ecommerce.cart.presentation.controller;

import com.yabozkurt.n11bootcamp.ecommerce.cart.application.service.CartService;
import com.yabozkurt.n11bootcamp.ecommerce.cart.domain.exception.CartValidationException;
import com.yabozkurt.n11bootcamp.ecommerce.cart.presentation.dto.request.AddCartItemRequest;
import com.yabozkurt.n11bootcamp.ecommerce.cart.presentation.dto.request.CouponPreviewRequest;
import com.yabozkurt.n11bootcamp.ecommerce.cart.presentation.dto.request.UpdateCartItemQuantityRequest;
import com.yabozkurt.n11bootcamp.ecommerce.cart.presentation.dto.response.ApiResponse;
import com.yabozkurt.n11bootcamp.ecommerce.cart.presentation.dto.response.CartResponse;
import com.yabozkurt.n11bootcamp.ecommerce.cart.presentation.dto.response.CouponPreviewResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cart")
@Tag(name = "Cart", description = "Cart item operations and coupon preview")
@SecurityRequirement(name = "cookieAuth")
public class CartController {

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @GetMapping
    @Operation(summary = "Get current user's cart")
    public ResponseEntity<ApiResponse<CartResponse>> getCart(
            @RequestHeader(name = "X-User-Id", required = false) String userIdHeader) {
        return ResponseEntity.ok(ApiResponse.ok(cartService.getCart(parseUserId(userIdHeader))));
    }

    @PostMapping("/items")
    @Operation(summary = "Add item to cart")
    public ResponseEntity<ApiResponse<CartResponse>> addItem(
            @RequestHeader(name = "X-User-Id", required = false) String userIdHeader,
            @Valid @RequestBody AddCartItemRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(cartService.addItem(parseUserId(userIdHeader), request)));
    }

    @PatchMapping("/items/{itemId}")
    @Operation(summary = "Update cart item quantity")
    public ResponseEntity<ApiResponse<CartResponse>> updateItemQuantity(
            @RequestHeader(name = "X-User-Id", required = false) String userIdHeader,
            @PathVariable String itemId,
            @Valid @RequestBody UpdateCartItemQuantityRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(cartService.updateItemQuantity(parseUserId(userIdHeader), itemId, request.getQuantity())));
    }

    @DeleteMapping("/items/{itemId}")
    @Operation(summary = "Remove item from cart")
    public ResponseEntity<ApiResponse<CartResponse>> removeItem(
            @RequestHeader(name = "X-User-Id", required = false) String userIdHeader,
            @PathVariable String itemId) {
        return ResponseEntity.ok(ApiResponse.ok(cartService.removeItem(parseUserId(userIdHeader), itemId)));
    }

    @DeleteMapping
    @Operation(summary = "Clear cart")
    public ResponseEntity<ApiResponse<Void>> clearCart(
            @RequestHeader(name = "X-User-Id", required = false) String userIdHeader) {
        cartService.clearCart(parseUserId(userIdHeader));
        return ResponseEntity.ok(ApiResponse.ok("Sepet temizlendi", null));
    }

    @PostMapping("/coupon/preview")
    @Operation(summary = "Preview coupon effect on cart total")
    public ResponseEntity<ApiResponse<CouponPreviewResponse>> previewCoupon(
            @RequestHeader(name = "X-User-Id", required = false) String userIdHeader,
            @Valid @RequestBody CouponPreviewRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(cartService.previewCoupon(parseUserId(userIdHeader), request)));
    }

    private Long parseUserId(String userIdHeader) {
        if (userIdHeader == null || userIdHeader.isBlank()) {
            throw new CartValidationException("X-User-Id header zorunlu");
        }
        try {
            return Long.valueOf(userIdHeader);
        } catch (NumberFormatException ex) {
            throw new CartValidationException("X-User-Id geçersiz");
        }
    }
}
