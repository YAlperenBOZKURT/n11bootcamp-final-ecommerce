package com.yabozkurt.n11bootcamp.ecommerce.product;

import com.yabozkurt.n11bootcamp.ecommerce.product.infrastructure.elasticsearch.service.ProductIndexService;
import com.yabozkurt.n11bootcamp.ecommerce.product.infrastructure.messaging.publisher.ProductEventPublisher;
import com.yabozkurt.n11bootcamp.ecommerce.product.infrastructure.minio.MinioService;
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
        "spring.config.import=optional:configserver:",
        "minio.url=http://localhost:9000",
        "minio.access-key=minioadmin",
        "minio.secret-key=minioadmin",
        "minio.bucket=products"
})
@ActiveProfiles("test")
@Testcontainers
class ProductServiceApplicationTests {

    @MockBean
    MinioService minioService;

    @MockBean
    ProductEventPublisher eventPublisher;

    @MockBean
    ProductIndexService productIndexService;

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("product_service_test")
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
