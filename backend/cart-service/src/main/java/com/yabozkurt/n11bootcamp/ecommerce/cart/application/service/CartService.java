package com.yabozkurt.n11bootcamp.ecommerce.cart.application.service;

import com.yabozkurt.n11bootcamp.ecommerce.cart.presentation.dto.request.AddCartItemRequest;
import com.yabozkurt.n11bootcamp.ecommerce.cart.presentation.dto.request.CouponPreviewRequest;
import com.yabozkurt.n11bootcamp.ecommerce.cart.presentation.dto.response.CartResponse;
import com.yabozkurt.n11bootcamp.ecommerce.cart.presentation.dto.response.CouponPreviewResponse;

public interface CartService {
    CartResponse getCart(Long userId);
    CartResponse addItem(Long userId, AddCartItemRequest request);
    CartResponse updateItemQuantity(Long userId, String itemId, Integer quantity);
    CartResponse removeItem(Long userId, String itemId);
    void clearCart(Long userId);
    CouponPreviewResponse previewCoupon(Long userId, CouponPreviewRequest request);
}
