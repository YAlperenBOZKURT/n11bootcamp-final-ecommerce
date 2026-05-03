package com.yabozkurt.n11bootcamp.ecommerce.order.application.service;

import com.yabozkurt.n11bootcamp.ecommerce.order.application.service.impl.OrderServiceImpl;
import com.yabozkurt.n11bootcamp.ecommerce.order.application.service.impl.SagaTransactionSupport;
import com.yabozkurt.n11bootcamp.ecommerce.order.domain.exception.OrderNotFoundException;
import com.yabozkurt.n11bootcamp.ecommerce.order.domain.exception.OrderValidationException;
import com.yabozkurt.n11bootcamp.ecommerce.order.domain.model.Order;
import com.yabozkurt.n11bootcamp.ecommerce.order.domain.model.enums.OrderStatus;
import com.yabozkurt.n11bootcamp.ecommerce.order.domain.repository.OrderRepository;
import com.yabozkurt.n11bootcamp.ecommerce.order.infrastructure.client.*;
import com.yabozkurt.n11bootcamp.ecommerce.order.infrastructure.client.dto.*;
import com.yabozkurt.n11bootcamp.ecommerce.order.infrastructure.messaging.publisher.OrderEventPublisher;
import com.yabozkurt.n11bootcamp.ecommerce.order.presentation.dto.request.CardRequest;
import com.yabozkurt.n11bootcamp.ecommerce.order.presentation.dto.request.OrderItemRequest;
import com.yabozkurt.n11bootcamp.ecommerce.order.presentation.dto.request.OrderRequest;
import com.yabozkurt.n11bootcamp.ecommerce.order.presentation.dto.response.OrderResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class OrderServiceImplTest {

    @Mock private OrderRepository orderRepository;
    @Mock private ProductClient productClient;
    @Mock private StockClient stockClient;
    @Mock private PaymentClient paymentClient;
    @Mock private CouponClient couponClient;
    @Mock private OrderEventPublisher eventPublisher;
    @Mock private SagaTransactionSupport sagaSupport;

    private OrderServiceImpl orderService;

    @BeforeEach
    void setUp() {
        orderService = new OrderServiceImpl(orderRepository, productClient, stockClient,
                paymentClient, couponClient, eventPublisher, sagaSupport);
    }

    private OrderRequest buildRequest() {
        OrderItemRequest item = new OrderItemRequest();
        item.setProductId(1L);
        item.setVariantId(10L);
        item.setQuantity(2);

        CardRequest card = new CardRequest();
        card.setCardHolderName("Test User");
        card.setCardNumber("4111111111111111");
        card.setExpireMonth("12");
        card.setExpireYear("2030");
        card.setCvc("123");

        OrderRequest req = new OrderRequest();
        req.setItems(List.of(item));
        req.setCard(card);
        return req;
    }

    private ApiResponse<ProductClientResponse> productResponse() {
        VariantClientResponse variant = new VariantClientResponse();
        variant.setId(10L);
        variant.setEffectivePrice(new BigDecimal("100.00"));

        ProductClientResponse p = new ProductClientResponse();
        p.setId(1L);
        p.setName("Test Ürün");
        p.setVariants(List.of(variant));
        ApiResponse<ProductClientResponse> resp = new ApiResponse<>();
        resp.setSuccess(true);
        resp.setData(p);
        return resp;
    }

    private Order savedOrder(OrderStatus status) {
        Order order = new Order();
        order.setOrderNumber("ORD-TEST123");
        order.setUserId(1L);
        order.setUserEmail("test@test.com");
        order.setTotalAmount(new BigDecimal("200.00"));
        order.setDiscountAmount(BigDecimal.ZERO);
        order.setFinalAmount(new BigDecimal("200.00"));
        order.setStatus(status);
        order.setItems(List.of());
        return order;
    }

    private ApiResponse<PaymentResponse> successPayment() {
        PaymentResponse pr = new PaymentResponse();
        pr.setStatus("SUCCESS");
        ApiResponse<PaymentResponse> resp = new ApiResponse<>();
        resp.setSuccess(true);
        resp.setData(pr);
        return resp;
    }

    @Test
    void placeOrder_success_priceFromProductService() {
        when(productClient.getById(1L)).thenReturn(productResponse());
        when(sagaSupport.savePending(any())).thenReturn(savedOrder(OrderStatus.PENDING));
        when(sagaSupport.markConfirmed(any(), any(), any(), any())).thenReturn(savedOrder(OrderStatus.CONFIRMED));
        when(stockClient.reserveVariant(anyLong(), any())).thenReturn(null);
        when(paymentClient.checkout(any())).thenReturn(successPayment());
        when(stockClient.confirmVariant(anyLong(), any())).thenReturn(null);

        OrderResponse response = orderService.placeOrder(1L, "test@test.com", buildRequest());

        assertThat(response).isNotNull();
        verify(productClient).getById(1L);
        verify(stockClient).reserveVariant(anyLong(), any());
        verify(paymentClient).checkout(any());
        verify(eventPublisher).publishOrderConfirmed(any());
    }

    @Test
    void placeOrder_paymentFails_releasesStock() {
        PaymentResponse pr = new PaymentResponse();
        pr.setStatus("FAILED");
        pr.setFailReason("Kart limiti yetersiz");
        ApiResponse<PaymentResponse> failResp = new ApiResponse<>();
        failResp.setSuccess(true);
        failResp.setData(pr);

        when(productClient.getById(anyLong())).thenReturn(productResponse());
        when(sagaSupport.savePending(any())).thenReturn(savedOrder(OrderStatus.PENDING));
        when(sagaSupport.markFailed(any(), any())).thenReturn(savedOrder(OrderStatus.FAILED));
        when(stockClient.reserveVariant(anyLong(), any())).thenReturn(null);
        when(paymentClient.checkout(any())).thenReturn(failResp);

        orderService.placeOrder(1L, "test@test.com", buildRequest());

        verify(stockClient).releaseVariant(anyLong(), any());
        verify(stockClient, never()).confirmVariant(anyLong(), any());
        verify(eventPublisher).publishOrderFailed(any());
    }

    @Test
    void placeOrder_productNotFound_throwsException() {
        ApiResponse<ProductClientResponse> notFound = new ApiResponse<>();
        notFound.setSuccess(false);
        when(productClient.getById(anyLong())).thenReturn(notFound);

        assertThatThrownBy(() -> orderService.placeOrder(1L, "test@test.com", buildRequest()))
                .isInstanceOf(OrderValidationException.class)
                .hasMessageContaining("Ürün bulunamadı");
    }

    @Test
    void placeOrder_stockFails_failsOrder() {
        when(productClient.getById(anyLong())).thenReturn(productResponse());
        when(sagaSupport.savePending(any())).thenReturn(savedOrder(OrderStatus.PENDING));
        when(sagaSupport.markFailed(any(), any())).thenReturn(savedOrder(OrderStatus.FAILED));
        when(stockClient.reserveVariant(anyLong(), any())).thenThrow(new RuntimeException("Yetersiz stok"));

        orderService.placeOrder(1L, "test@test.com", buildRequest());

        verify(paymentClient, never()).checkout(any());
        verify(eventPublisher).publishOrderFailed(any());
    }

    @Test
    void getByOrderNumber_ownerCanAccess() {
        Order order = savedOrder(OrderStatus.CONFIRMED);
        when(orderRepository.findByOrderNumber("ORD-TEST123")).thenReturn(Optional.of(order));

        OrderResponse response = orderService.getByOrderNumber(1L, null, "ORD-TEST123");

        assertThat(response.getOrderNumber()).isEqualTo("ORD-TEST123");
    }

    @Test
    void getByOrderNumber_adminCanAccessAnyOrder() {
        Order order = savedOrder(OrderStatus.CONFIRMED);
        when(orderRepository.findByOrderNumber("ORD-TEST123")).thenReturn(Optional.of(order));

        assertThat(orderService.getByOrderNumber(99L, "ADMIN", "ORD-TEST123")).isNotNull();
    }

    @Test
    void getByOrderNumber_wrongUser_throwsNotFound() {
        Order order = savedOrder(OrderStatus.CONFIRMED);
        when(orderRepository.findByOrderNumber("ORD-TEST123")).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.getByOrderNumber(99L, null, "ORD-TEST123"))
                .isInstanceOf(OrderNotFoundException.class);
    }

    @Test
    void getByOrderNumber_notFound_throwsException() {
        when(orderRepository.findByOrderNumber("NOTEXIST")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.getByOrderNumber(1L, null, "NOTEXIST"))
                .isInstanceOf(OrderNotFoundException.class);
    }

    @Test
    void getOrdersByUser_returnsAll() {
        when(orderRepository.findByUserIdWithItems(1L)).thenReturn(List.of(
                savedOrder(OrderStatus.CONFIRMED), savedOrder(OrderStatus.PENDING)));

        assertThat(orderService.getOrdersByUser(1L)).hasSize(2);
    }

    @Test
    void getAllOrders_returnsPaginatedResults() {
        PageRequest pageable = PageRequest.of(0, 10);
        Order o1 = savedOrder(OrderStatus.CONFIRMED);
        Order o2 = savedOrder(OrderStatus.CANCELLED);
        o1.setId(1L);
        o2.setId(2L);
        Page<Long> idPage = new PageImpl<>(List.of(1L, 2L), pageable, 2);
        when(orderRepository.findAllOrderIds(pageable)).thenReturn(idPage);
        when(orderRepository.findAllByIdInWithItems(List.of(1L, 2L))).thenReturn(List.of(o1, o2));

        Page<OrderResponse> result = orderService.getAllOrders(pageable);

        assertThat(result.getTotalElements()).isEqualTo(2);
        assertThat(result.getContent()).hasSize(2);
    }

    @Test
    void cancelOrder_success() {
        Order order = savedOrder(OrderStatus.PENDING);
        when(orderRepository.findByOrderNumber("ORD-TEST123")).thenReturn(Optional.of(order));
        when(orderRepository.save(any())).thenReturn(savedOrder(OrderStatus.CANCELLED));

        orderService.cancelOrder(1L, "ORD-TEST123");

        verify(eventPublisher).publishOrderCancelled(any());
    }

    @Test
    void cancelOrder_wrongUser_throwsNotFound() {
        Order order = savedOrder(OrderStatus.PENDING);
        when(orderRepository.findByOrderNumber("ORD-TEST123")).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.cancelOrder(99L, "ORD-TEST123"))
                .isInstanceOf(OrderNotFoundException.class);
    }

    @Test
    void cancelOrder_confirmedOrder_throwsIllegalState() {
        Order order = savedOrder(OrderStatus.CONFIRMED);
        when(orderRepository.findByOrderNumber("ORD-TEST123")).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.cancelOrder(1L, "ORD-TEST123"))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void cancelOrder_alreadyCancelled_throwsException() {
        Order order = savedOrder(OrderStatus.CANCELLED);
        when(orderRepository.findByOrderNumber("ORD-TEST123")).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.cancelOrder(1L, "ORD-TEST123"))
                .isInstanceOf(IllegalStateException.class);
    }
}
