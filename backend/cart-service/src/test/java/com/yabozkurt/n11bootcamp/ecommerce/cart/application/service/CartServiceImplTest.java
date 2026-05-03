package com.yabozkurt.n11bootcamp.ecommerce.cart.application.service;

import com.yabozkurt.n11bootcamp.ecommerce.cart.application.service.impl.CartServiceImpl;
import com.yabozkurt.n11bootcamp.ecommerce.cart.domain.exception.CartItemNotFoundException;
import com.yabozkurt.n11bootcamp.ecommerce.cart.domain.model.Cart;
import com.yabozkurt.n11bootcamp.ecommerce.cart.domain.repository.CartRepository;
import com.yabozkurt.n11bootcamp.ecommerce.cart.infrastructure.client.CouponClient;
import com.yabozkurt.n11bootcamp.ecommerce.cart.infrastructure.client.dto.ApiResponse;
import com.yabozkurt.n11bootcamp.ecommerce.cart.infrastructure.client.dto.CouponValidationData;
import com.yabozkurt.n11bootcamp.ecommerce.cart.presentation.dto.request.AddCartItemRequest;
import com.yabozkurt.n11bootcamp.ecommerce.cart.presentation.dto.request.CouponPreviewRequest;
import com.yabozkurt.n11bootcamp.ecommerce.cart.presentation.dto.response.CartResponse;
import com.yabozkurt.n11bootcamp.ecommerce.cart.presentation.dto.response.CouponPreviewResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CartServiceImplTest {

    @Mock private CartRepository cartRepository;
    @Mock private CouponClient couponClient;

    private CartServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new CartServiceImpl(cartRepository, couponClient);
    }

    @Test
    void addItem_shouldCreateAndReturnCart() {
        when(cartRepository.findById(101L)).thenReturn(Optional.empty());
        when(cartRepository.save(any(Cart.class))).thenAnswer(inv -> inv.getArgument(0));

        AddCartItemRequest req = new AddCartItemRequest();
        setField(req, "productId", 1L);
        setField(req, "productName", "Sneaker");
        setField(req, "unitPrice", BigDecimal.valueOf(250));
        setField(req, "quantity", 2);

        CartResponse response = service.addItem(101L, req);

        assertEquals(1, response.getItems().size());
        assertEquals(BigDecimal.valueOf(500), response.getTotalAmount());
    }

    @Test
    void removeItem_shouldThrow_whenMissing() {
        Cart cart = new Cart();
        cart.setUserId(101L);
        when(cartRepository.findById(101L)).thenReturn(Optional.of(cart));

        assertThrows(CartItemNotFoundException.class, () -> service.removeItem(101L, "nope"));
    }

    @Test
    void previewCoupon_shouldReturnDiscountedTotals() {
        Cart cart = new Cart();
        cart.setUserId(101L);
        when(cartRepository.findById(101L)).thenReturn(Optional.of(cart));
        when(cartRepository.save(any(Cart.class))).thenAnswer(inv -> inv.getArgument(0));

        AddCartItemRequest addReq = new AddCartItemRequest();
        setField(addReq, "productId", 9L);
        setField(addReq, "productName", "T-Shirt");
        setField(addReq, "unitPrice", BigDecimal.valueOf(100));
        setField(addReq, "quantity", 3);
        service.addItem(101L, addReq);

        CouponValidationData data = new CouponValidationData();
        setField(data, "code", "WELCOME10");
        setField(data, "discountAmount", BigDecimal.valueOf(30));
        setField(data, "finalAmount", BigDecimal.valueOf(270));
        ApiResponse<CouponValidationData> api = new ApiResponse<>();
        setField(api, "success", true);
        setField(api, "data", data);
        when(couponClient.validate(any(), any())).thenReturn(api);

        CouponPreviewRequest previewReq = new CouponPreviewRequest();
        setField(previewReq, "code", "WELCOME10");

        CouponPreviewResponse response = service.previewCoupon(101L, previewReq);
        assertEquals(BigDecimal.valueOf(300), response.getCartTotal());
        assertEquals(BigDecimal.valueOf(30), response.getDiscountAmount());
        assertEquals(BigDecimal.valueOf(270), response.getFinalAmount());
    }

    private static void setField(Object target, String fieldName, Object value) {
        try {
            Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
