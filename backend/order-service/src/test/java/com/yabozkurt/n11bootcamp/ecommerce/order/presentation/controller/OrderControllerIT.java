package com.yabozkurt.n11bootcamp.ecommerce.order.presentation.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yabozkurt.n11bootcamp.ecommerce.order.domain.model.Order;
import com.yabozkurt.n11bootcamp.ecommerce.order.domain.model.enums.OrderStatus;
import com.yabozkurt.n11bootcamp.ecommerce.order.domain.repository.OrderRepository;
import com.yabozkurt.n11bootcamp.ecommerce.order.infrastructure.client.*;
import com.yabozkurt.n11bootcamp.ecommerce.order.infrastructure.client.dto.*;
import com.yabozkurt.n11bootcamp.ecommerce.order.infrastructure.messaging.publisher.OrderEventPublisher;
import com.yabozkurt.n11bootcamp.ecommerce.order.presentation.dto.request.CardRequest;
import com.yabozkurt.n11bootcamp.ecommerce.order.presentation.dto.request.OrderItemRequest;
import com.yabozkurt.n11bootcamp.ecommerce.order.presentation.dto.request.OrderRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "spring.cloud.config.enabled=false",
                "spring.config.import="
        })
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Testcontainers
class OrderControllerIT {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("orderdb")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void overrideProps(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
    }

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired OrderRepository orderRepository;

    @MockBean ProductClient productClient;
    @MockBean StockClient stockClient;
    @MockBean PaymentClient paymentClient;
    @MockBean CouponClient couponClient;
    @MockBean OrderEventPublisher eventPublisher;

    @BeforeEach
    void setUp() {
        orderRepository.deleteAll();
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

    private ApiResponse<ProductClientResponse> productOk() {
        VariantClientResponse variant = new VariantClientResponse();
        variant.setId(10L);
        variant.setEffectivePrice(new BigDecimal("150.00"));

        ProductClientResponse p = new ProductClientResponse();
        p.setId(1L);
        p.setName("Test Ürün");
        p.setVariants(List.of(variant));
        ApiResponse<ProductClientResponse> resp = new ApiResponse<>();
        resp.setSuccess(true);
        resp.setData(p);
        return resp;
    }

    private ApiResponse<PaymentResponse> successPayment() {
        PaymentResponse pr = new PaymentResponse();
        pr.setStatus("SUCCESS");
        ApiResponse<PaymentResponse> resp = new ApiResponse<>();
        resp.setSuccess(true);
        resp.setData(pr);
        return resp;
    }

    private Order savePendingOrder(Long userId, String email, String orderNumber) {
        Order order = new Order();
        order.setOrderNumber(orderNumber);
        order.setUserId(userId);
        order.setUserEmail(email);
        order.setStatus(OrderStatus.PENDING);
        order.setTotalAmount(new BigDecimal("300.00"));
        order.setDiscountAmount(BigDecimal.ZERO);
        order.setFinalAmount(new BigDecimal("300.00"));
        return orderRepository.save(order);
    }

    // -- placeOrder ------------------------------------------------------------

    @Test
    void placeOrder_success_returns201() throws Exception {
        when(productClient.getById(anyLong())).thenReturn(productOk());
        when(stockClient.reserveVariant(anyLong(), any())).thenReturn(null);
        when(paymentClient.checkout(any())).thenReturn(successPayment());
        when(stockClient.confirmVariant(anyLong(), any())).thenReturn(null);

        mockMvc.perform(post("/api/orders")
                        .header("X-User-Id", "1")
                        .header("X-User-Email", "user@test.com")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildRequest())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("CONFIRMED"));

        assertThat(orderRepository.findAll()).hasSize(1);
    }

    @Test
    void placeOrder_paymentFails_returns201WithFailedStatus() throws Exception {
        PaymentResponse pr = new PaymentResponse();
        pr.setStatus("FAILED");
        pr.setFailReason("Kart limiti yetersiz");
        ApiResponse<PaymentResponse> failResp = new ApiResponse<>();
        failResp.setSuccess(true);
        failResp.setData(pr);

        when(productClient.getById(anyLong())).thenReturn(productOk());
        when(stockClient.reserveVariant(anyLong(), any())).thenReturn(null);
        when(paymentClient.checkout(any())).thenReturn(failResp);
        when(stockClient.releaseVariant(anyLong(), any())).thenReturn(null);

        mockMvc.perform(post("/api/orders")
                        .header("X-User-Id", "1")
                        .header("X-User-Email", "user@test.com")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildRequest())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("FAILED"));
    }

    @Test
    void placeOrder_missingUserId_returns400() throws Exception {
        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildRequest())))
                .andExpect(status().isBadRequest());
    }

    // -- getByOrderNumber ------------------------------------------------------

    @Test
    void getByOrderNumber_ownerCanAccess() throws Exception {
        when(productClient.getById(anyLong())).thenReturn(productOk());
        when(stockClient.reserveVariant(anyLong(), any())).thenReturn(null);
        when(paymentClient.checkout(any())).thenReturn(successPayment());
        when(stockClient.confirmVariant(anyLong(), any())).thenReturn(null);

        String body = mockMvc.perform(post("/api/orders")
                        .header("X-User-Id", "1")
                        .header("X-User-Email", "user@test.com")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildRequest())))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String orderNumber = objectMapper.readTree(body).path("data").path("orderNumber").asText();

        mockMvc.perform(get("/api/orders/{orderNumber}", orderNumber)
                        .header("X-User-Id", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.orderNumber").value(orderNumber));
    }

    @Test
    void getByOrderNumber_wrongUser_returns404() throws Exception {
        when(productClient.getById(anyLong())).thenReturn(productOk());
        when(stockClient.reserveVariant(anyLong(), any())).thenReturn(null);
        when(paymentClient.checkout(any())).thenReturn(successPayment());
        when(stockClient.confirmVariant(anyLong(), any())).thenReturn(null);

        String body = mockMvc.perform(post("/api/orders")
                        .header("X-User-Id", "1")
                        .header("X-User-Email", "user@test.com")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildRequest())))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        String orderNumber = objectMapper.readTree(body).path("data").path("orderNumber").asText();

        mockMvc.perform(get("/api/orders/{orderNumber}", orderNumber)
                        .header("X-User-Id", "99"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getByOrderNumber_notFound_returns404() throws Exception {
        mockMvc.perform(get("/api/orders/ORD-NOTEXIST")
                        .header("X-User-Id", "1"))
                .andExpect(status().isNotFound());
    }

    // -- getMyOrders -----------------------------------------------------------

    @Test
    void getMyOrders_returnsUserOrders() throws Exception {
        when(productClient.getById(anyLong())).thenReturn(productOk());
        when(stockClient.reserveVariant(anyLong(), any())).thenReturn(null);
        when(paymentClient.checkout(any())).thenReturn(successPayment());
        when(stockClient.confirmVariant(anyLong(), any())).thenReturn(null);

        mockMvc.perform(post("/api/orders")
                        .header("X-User-Id", "5")
                        .header("X-User-Email", "user5@test.com")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildRequest())))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/orders/my").header("X-User-Id", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1));
    }

    // -- cancelOrder -----------------------------------------------------------

    @Test
    void cancelOrder_success() throws Exception {
        Order pending = savePendingOrder(1L, "user@test.com", "ORD-CANCEL-TEST");

        mockMvc.perform(post("/api/orders/{orderNumber}/cancel", pending.getOrderNumber())
                        .header("X-User-Id", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CANCELLED"));
    }

    @Test
    void cancelOrder_alreadyCancelled_returns409() throws Exception {
        Order pending = savePendingOrder(1L, "user@test.com", "ORD-CANCEL-TWICE");

        mockMvc.perform(post("/api/orders/{orderNumber}/cancel", pending.getOrderNumber())
                        .header("X-User-Id", "1"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/orders/{orderNumber}/cancel", pending.getOrderNumber())
                        .header("X-User-Id", "1"))
                .andExpect(status().isConflict());
    }
}
