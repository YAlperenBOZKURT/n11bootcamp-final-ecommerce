package com.yabozkurt.n11bootcamp.stock.presentation.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yabozkurt.n11bootcamp.stock.application.service.StockService;
import com.yabozkurt.n11bootcamp.stock.domain.repository.StockMovementRepository;
import com.yabozkurt.n11bootcamp.stock.domain.repository.StockRepository;
import com.yabozkurt.n11bootcamp.stock.infrastructure.messaging.publisher.StockEventPublisher;
import com.yabozkurt.n11bootcamp.stock.presentation.dto.request.ReserveRequest;
import com.yabozkurt.n11bootcamp.stock.presentation.dto.request.StockUpdateRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "spring.cloud.config.enabled=false",
                "spring.config.import=optional:configserver:",
                "rabbitmq.queues.product-created=product.created"
        }
)
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
class StockControllerIT {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("stock_service_test")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void overrideProperties(DynamicPropertyRegistry registry) {
        registry.add("DATASOURCE_URL", postgres::getJdbcUrl);
        registry.add("DATASOURCE_USERNAME", postgres::getUsername);
        registry.add("DATASOURCE_PASSWORD", postgres::getPassword);
    }

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @Autowired StockService stockService;
    @Autowired StockRepository stockRepository;
    @Autowired StockMovementRepository movementRepository;
    @MockBean StockEventPublisher stockEventPublisher;

    @BeforeEach
    void setUp() {
        movementRepository.deleteAll();
        stockRepository.deleteAll();
        stockService.initVariantStock(1L, 101L);
    }

    @Test
    void getByVariantId_existing_returns200() throws Exception {
        mockMvc.perform(get("/api/stocks/variants/101"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.productId").value(1))
                .andExpect(jsonPath("$.data.variantId").value(101))
                .andExpect(jsonPath("$.data.quantity").value(0));
    }

    @Test
    void getByVariantId_notFound_returns404() throws Exception {
        mockMvc.perform(get("/api/stocks/variants/999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void addStock_increasesQuantity() throws Exception {
        StockUpdateRequest req = new StockUpdateRequest();
        req.setQuantity(50);

        mockMvc.perform(post("/api/stocks/variants/101/add")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.quantity").value(50))
                .andExpect(jsonPath("$.data.availableQuantity").value(50));
    }

    @Test
    void reserve_sufficient_reservesQuantity() throws Exception {
        StockUpdateRequest addReq = new StockUpdateRequest();
        addReq.setQuantity(30);
        mockMvc.perform(post("/api/stocks/variants/101/add")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(addReq)));

        ReserveRequest req = new ReserveRequest();
        req.setQuantity(10);

        mockMvc.perform(post("/api/stocks/variants/101/reserve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.reservedQuantity").value(10))
                .andExpect(jsonPath("$.data.availableQuantity").value(20));
    }

    @Test
    void reserve_insufficient_returns409() throws Exception {
        ReserveRequest req = new ReserveRequest();
        req.setQuantity(100);

        mockMvc.perform(post("/api/stocks/variants/101/reserve")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isConflict());
    }

    @Test
    void getMovements_returnsHistory() throws Exception {
        StockUpdateRequest addReq = new StockUpdateRequest();
        addReq.setQuantity(20);
        mockMvc.perform(post("/api/stocks/variants/101/add")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(addReq)));

        mockMvc.perform(get("/api/stocks/variants/101/movements"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].type").value("IN"));
    }

    @Test
    void release_decreasesReserved() throws Exception {
        StockUpdateRequest addReq = new StockUpdateRequest();
        addReq.setQuantity(30);
        mockMvc.perform(post("/api/stocks/variants/101/add")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(addReq)));

        ReserveRequest reserveReq = new ReserveRequest();
        reserveReq.setQuantity(10);
        mockMvc.perform(post("/api/stocks/variants/101/reserve")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(reserveReq)));

        ReserveRequest releaseReq = new ReserveRequest();
        releaseReq.setQuantity(5);
        mockMvc.perform(post("/api/stocks/variants/101/release")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(releaseReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.reservedQuantity").value(5));
    }
}
