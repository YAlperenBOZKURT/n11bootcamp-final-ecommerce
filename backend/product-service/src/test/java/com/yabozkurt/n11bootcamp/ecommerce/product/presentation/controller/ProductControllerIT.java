package com.yabozkurt.n11bootcamp.ecommerce.product.presentation.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yabozkurt.n11bootcamp.ecommerce.product.domain.repository.CategoryRepository;
import com.yabozkurt.n11bootcamp.ecommerce.product.domain.repository.ProductRepository;
import com.yabozkurt.n11bootcamp.ecommerce.product.infrastructure.elasticsearch.service.ProductIndexService;
import com.yabozkurt.n11bootcamp.ecommerce.product.infrastructure.messaging.publisher.ProductEventPublisher;
import com.yabozkurt.n11bootcamp.ecommerce.product.infrastructure.minio.MinioService;
import com.yabozkurt.n11bootcamp.ecommerce.product.presentation.dto.request.CategoryRequest;
import com.yabozkurt.n11bootcamp.ecommerce.product.presentation.dto.request.ProductRequest;
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
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
                "spring.cloud.config.enabled=false",
                "spring.config.import=optional:configserver:"
        }
)
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
class ProductControllerIT {

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

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @MockBean MinioService minioService;
    @MockBean ProductEventPublisher eventPublisher;
    @MockBean ProductIndexService productIndexService;
    @Autowired ProductRepository productRepository;
    @Autowired CategoryRepository categoryRepository;

    private Long defaultCategoryId;

    @BeforeEach
    void setUp() throws Exception {
        productRepository.deleteAll();
        categoryRepository.deleteAll();

        CategoryRequest catReq = new CategoryRequest();
        catReq.setName("Default");
        String catJson = mockMvc.perform(post("/api/products/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(catReq)))
                .andReturn().getResponse().getContentAsString();
        defaultCategoryId = objectMapper.readTree(catJson).path("data").path("id").asLong();
    }

    @Test
    void createProduct_valid_returns200() throws Exception {
        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildRequest("Laptop"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Laptop"))
                .andExpect(jsonPath("$.data.status").value("PASSIVE"));
    }

    @Test
    void getById_existing_returns200() throws Exception {
        MvcResult created = mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildRequest("Telefon"))))
                .andReturn();

        Long id = objectMapper.readTree(created.getResponse().getContentAsString())
                .path("data").path("id").asLong();

        mockMvc.perform(get("/api/products/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Telefon"));
    }

    @Test
    void getById_notFound_returns404() throws Exception {
        mockMvc.perform(get("/api/products/999999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateProduct_changesFields() throws Exception {
        MvcResult created = mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildRequest("Eski"))))
                .andReturn();

        Long id = objectMapper.readTree(created.getResponse().getContentAsString())
                .path("data").path("id").asLong();

        mockMvc.perform(put("/api/products/" + id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildRequest("Yeni"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Yeni"));
    }

    @Test
    void deleteProduct_softDeletes() throws Exception {
        MvcResult created = mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildRequest("Silinecek"))))
                .andReturn();

        Long id = objectMapper.readTree(created.getResponse().getContentAsString())
                .path("data").path("id").asLong();

        mockMvc.perform(delete("/api/products/" + id)).andExpect(status().isOk());
        mockMvc.perform(get("/api/products/" + id)).andExpect(status().isNotFound());
    }

    @Test
    void getAll_returnsPaginatedProducts() throws Exception {
        mockMvc.perform(post("/api/products").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(buildRequest("P1")))).andExpect(status().isOk());
        mockMvc.perform(post("/api/products").contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(buildRequest("P2")))).andExpect(status().isOk());

        mockMvc.perform(get("/api/products/admin/search"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(2));
    }

    @Test
    void createProduct_withCategory_returnsCategory() throws Exception {
        CategoryRequest catReq = new CategoryRequest();
        catReq.setName("Elektronik");
        String catJson = mockMvc.perform(post("/api/products/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(catReq)))
                .andReturn().getResponse().getContentAsString();
        Long categoryId = objectMapper.readTree(catJson).path("data").path("id").asLong();

        ProductRequest req = buildRequest("Laptop");
        req.setCategoryId(categoryId);

        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.category.name").value("Elektronik"));
    }

    private ProductRequest buildRequest(String name) {
        ProductRequest req = new ProductRequest();
        req.setName(name);
        req.setDescription("Test description");
        req.setBrand("TestBrand");
        req.setCategoryId(defaultCategoryId);
        return req;
    }
}
