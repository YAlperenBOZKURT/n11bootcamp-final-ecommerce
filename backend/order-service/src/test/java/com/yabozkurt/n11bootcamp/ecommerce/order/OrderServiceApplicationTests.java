package com.yabozkurt.n11bootcamp.ecommerce.order;

import com.yabozkurt.n11bootcamp.ecommerce.order.infrastructure.client.CouponClient;
import com.yabozkurt.n11bootcamp.ecommerce.order.infrastructure.client.PaymentClient;
import com.yabozkurt.n11bootcamp.ecommerce.order.infrastructure.client.ProductClient;
import com.yabozkurt.n11bootcamp.ecommerce.order.infrastructure.client.StockClient;
import com.yabozkurt.n11bootcamp.ecommerce.order.infrastructure.messaging.publisher.OrderEventPublisher;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest(properties = {
        "spring.cloud.config.enabled=false",
        "spring.config.import="
})
@ActiveProfiles("test")
@Testcontainers
class OrderServiceApplicationTests {

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

    @MockBean
    ProductClient productClient;

    @MockBean
    StockClient stockClient;

    @MockBean
    PaymentClient paymentClient;

    @MockBean
    CouponClient couponClient;

    @MockBean
    OrderEventPublisher eventPublisher;

    @Test
    void contextLoads() {
    }
}
