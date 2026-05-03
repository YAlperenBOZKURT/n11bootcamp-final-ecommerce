package com.yabozkurt.n11bootcamp.ecommerce.cart.application.service.impl;

import com.yabozkurt.n11bootcamp.ecommerce.cart.application.service.CartService;
import com.yabozkurt.n11bootcamp.ecommerce.cart.domain.exception.CartItemNotFoundException;
import com.yabozkurt.n11bootcamp.ecommerce.cart.domain.exception.CartValidationException;
import com.yabozkurt.n11bootcamp.ecommerce.cart.domain.model.Cart;
import com.yabozkurt.n11bootcamp.ecommerce.cart.domain.model.CartItem;
import com.yabozkurt.n11bootcamp.ecommerce.cart.domain.repository.CartRepository;
import com.yabozkurt.n11bootcamp.ecommerce.cart.infrastructure.client.CouponClient;
import com.yabozkurt.n11bootcamp.ecommerce.cart.infrastructure.client.dto.ApiResponse;
import com.yabozkurt.n11bootcamp.ecommerce.cart.infrastructure.client.dto.CouponValidateRequest;
import com.yabozkurt.n11bootcamp.ecommerce.cart.infrastructure.client.dto.CouponValidationData;
import com.yabozkurt.n11bootcamp.ecommerce.cart.presentation.dto.request.AddCartItemRequest;
import com.yabozkurt.n11bootcamp.ecommerce.cart.presentation.dto.request.CouponPreviewRequest;
import com.yabozkurt.n11bootcamp.ecommerce.cart.presentation.dto.response.CartItemResponse;
import com.yabozkurt.n11bootcamp.ecommerce.cart.presentation.dto.response.CartResponse;
import com.yabozkurt.n11bootcamp.ecommerce.cart.presentation.dto.response.CouponPreviewResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class CartServiceImpl implements CartService {

    private static final Logger log = LoggerFactory.getLogger(CartServiceImpl.class);

    private final CartRepository cartRepository;
    private final CouponClient couponClient;

    public CartServiceImpl(CartRepository cartRepository, CouponClient couponClient) {
        this.cartRepository = cartRepository;
        this.couponClient = couponClient;
    }

    @Override
    public CartResponse getCart(Long userId) {
        return toResponse(getOrCreateCart(userId));
    }

    @Override
    public CartResponse addItem(Long userId, AddCartItemRequest request) {
        Cart cart = getOrCreateCart(userId);
        CartItem existing = cart.getItems().stream()
                .filter(i -> i.getProductId().equals(request.getProductId())
                        && equalVariant(i.getVariantId(), request.getVariantId()))
                .findFirst()
                .orElse(null);

        if (existing == null) {
            CartItem item = new CartItem();
            item.setProductId(request.getProductId());
            item.setVariantId(request.getVariantId());
            item.setProductName(request.getProductName());
            item.setUnitPrice(request.getUnitPrice());
            item.setQuantity(request.getQuantity());
            cart.getItems().add(item);
            log.info("Cart item added: userId={}, productId={}, variantId={}, qty={}", userId, request.getProductId(), request.getVariantId(), request.getQuantity());
        } else {
            existing.setQuantity(existing.getQuantity() + request.getQuantity());
            existing.setUnitPrice(request.getUnitPrice());
            existing.setProductName(request.getProductName());
            log.info("Cart item quantity updated: userId={}, productId={}, newQty={}", userId, request.getProductId(), existing.getQuantity());
        }

        cart.setUpdatedAt(LocalDateTime.now());
        return toResponse(cartRepository.save(cart));
    }

    @Override
    public CartResponse updateItemQuantity(Long userId, String itemId, Integer quantity) {
        Cart cart = getOrCreateCart(userId);
        CartItem item = cart.getItems().stream()
                .filter(i -> i.getItemId().equals(itemId))
                .findFirst()
                .orElseThrow(() -> new CartItemNotFoundException(itemId));
        item.setQuantity(quantity);
        cart.setUpdatedAt(LocalDateTime.now());
        return toResponse(cartRepository.save(cart));
    }

    @Override
    public CartResponse removeItem(Long userId, String itemId) {
        Cart cart = getOrCreateCart(userId);
        boolean removed = cart.getItems().removeIf(i -> i.getItemId().equals(itemId));
        if (!removed) {
            throw new CartItemNotFoundException(itemId);
        }
        cart.setUpdatedAt(LocalDateTime.now());
        return toResponse(cartRepository.save(cart));
    }

    @Override
    public void clearCart(Long userId) {
        Cart cart = getOrCreateCart(userId);
        cart.setItems(new ArrayList<>());
        cart.setUpdatedAt(LocalDateTime.now());
        cartRepository.save(cart);
        log.info("Cart cleared: userId={}", userId);
    }

    @Override
    public CouponPreviewResponse previewCoupon(Long userId, CouponPreviewRequest request) {
        Cart cart = getOrCreateCart(userId);
        if (cart.getItems().isEmpty()) {
            throw new CartValidationException("Boş sepete kupon önizlemesi yapılamaz");
        }

        ApiResponse<CouponValidationData> result = couponClient.validate(
                String.valueOf(userId),
                new CouponValidateRequest(request.getCode(), cart.totalAmount())
        );

        if (result == null || !result.isSuccess() || result.getData() == null) {
            throw new CartValidationException(result != null ? result.getMessage() : "Kupon doğrulama başarısız");
        }

        CouponValidationData data = result.getData();
        CouponPreviewResponse response = new CouponPreviewResponse();
        response.setCode(data.getCode());
        response.setCartTotal(cart.totalAmount());
        response.setDiscountAmount(data.getDiscountAmount());
        response.setFinalAmount(data.getFinalAmount());
        return response;
    }

    private Cart getOrCreateCart(Long userId) {
        return cartRepository.findById(userId).orElseGet(() -> {
            Cart cart = new Cart();
            cart.setUserId(userId);
            cart.setUpdatedAt(LocalDateTime.now());
            return cartRepository.save(cart);
        });
    }

    private static boolean equalVariant(Long a, Long b) {
        if (a == null && b == null) return true;
        if (a == null || b == null) return false;
        return a.equals(b);
    }

    private static CartResponse toResponse(Cart cart) {
        CartResponse response = new CartResponse();
        response.setUserId(cart.getUserId());
        List<CartItemResponse> items = cart.getItems().stream().map(i -> {
            CartItemResponse r = new CartItemResponse();
            r.setItemId(i.getItemId());
            r.setProductId(i.getProductId());
            r.setVariantId(i.getVariantId());
            r.setProductName(i.getProductName());
            r.setUnitPrice(i.getUnitPrice());
            r.setQuantity(i.getQuantity());
            r.setLineTotal(i.lineTotal());
            return r;
        }).toList();
        response.setItems(items);
        response.setTotalAmount(cart.totalAmount());
        return response;
    }
}
