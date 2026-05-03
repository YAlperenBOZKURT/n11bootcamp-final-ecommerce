package com.yabozkurt.n11bootcamp.stock;

import com.yabozkurt.n11bootcamp.stock.infrastructure.messaging.publisher.StockEventPublisher;
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
        "spring.config.import=optional:configserver:"
})
@ActiveProfiles("test")
@Testcontainers
class StockServiceApplicationTests {
    @MockBean
    StockEventPublisher stockEventPublisher;

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

    @Test
    void contextLoads() {}
}
