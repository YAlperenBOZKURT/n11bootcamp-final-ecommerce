package com.yabozkurt.n11bootcamp.coupon.presentation.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yabozkurt.n11bootcamp.coupon.domain.model.enums.CouponType;
import com.yabozkurt.n11bootcamp.coupon.domain.repository.CouponClaimRepository;
import com.yabozkurt.n11bootcamp.coupon.domain.repository.CouponRepository;
import com.yabozkurt.n11bootcamp.coupon.domain.repository.CouponUseRepository;
import com.yabozkurt.n11bootcamp.coupon.presentation.dto.request.ClaimCouponRequest;
import com.yabozkurt.n11bootcamp.coupon.presentation.dto.request.CreateCouponRequest;
import com.yabozkurt.n11bootcamp.coupon.presentation.dto.request.ValidateCouponRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.LocalDateTime;

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
class CouponControllerIT {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("coupondb_test")
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
    @Autowired CouponRepository couponRepository;
    @Autowired CouponClaimRepository claimRepository;
    @Autowired CouponUseRepository useRepository;

    @BeforeEach
    void cleanUp() {
        useRepository.deleteAll();
        claimRepository.deleteAll();
        couponRepository.deleteAll();
    }

    // -- create ----------------------------------------------------------------

    @Test
    void create_adminRole_succeeds() throws Exception {
        mockMvc.perform(post("/api/coupons")
                        .header("X-User-Role", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildCreateRequest("SAVE10"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.code").value("SAVE10"));
    }

    @Test
    void create_nonAdminRole_returns403() throws Exception {
        mockMvc.perform(post("/api/coupons")
                        .header("X-User-Role", "USER")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildCreateRequest("SAVE20"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void create_duplicateCode_returns400() throws Exception {
        createCoupon("DUPE");

        mockMvc.perform(post("/api/coupons")
                        .header("X-User-Role", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildCreateRequest("DUPE"))))
                .andExpect(status().isBadRequest());
    }

    // -- getById ---------------------------------------------------------------

    @Test
    void getById_exists_returnsCoupon() throws Exception {
        Long id = createCouponAndGetId("GET10");

        mockMvc.perform(get("/api/coupons/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.code").value("GET10"));
    }

    @Test
    void getById_notFound_returns404() throws Exception {
        mockMvc.perform(get("/api/coupons/9999"))
                .andExpect(status().isNotFound());
    }

    // -- getAll ----------------------------------------------------------------

    @Test
    void getAll_returnsPagedResults() throws Exception {
        createCoupon("LIST1");
        createCoupon("LIST2");

        mockMvc.perform(get("/api/coupons"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(2));
    }

    // -- claim -----------------------------------------------------------------

    @Test
    void claim_validCoupon_succeeds() throws Exception {
        createCouponOfType("CLAIM10", CouponType.USER);

        ClaimCouponRequest req = new ClaimCouponRequest();
        setCode(req, "CLAIM10");

        mockMvc.perform(post("/api/coupons/claim")
                        .header("X-User-Id", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void claim_missingUserId_returns400() throws Exception {
        ClaimCouponRequest req = new ClaimCouponRequest();
        setCode(req, "ANYCOUPON");

        mockMvc.perform(post("/api/coupons/claim")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    // -- validate --------------------------------------------------------------

    @Test
    void validate_globalCoupon_noClaimNeeded_returnsDiscount() throws Exception {
        createCoupon("GLOBAL10");

        ValidateCouponRequest req = new ValidateCouponRequest();
        setCode(req, "GLOBAL10");
        setOrderAmount(req, new BigDecimal("200.00"));

        mockMvc.perform(post("/api/coupons/validate")
                        .header("X-User-Id", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.code").value("GLOBAL10"))
                .andExpect(jsonPath("$.data.discountAmount").isNumber());
    }

    @Test
    void validate_userTypeCoupon_notClaimed_returns400() throws Exception {
        createCouponOfType("USERONLY", CouponType.USER);

        ValidateCouponRequest req = new ValidateCouponRequest();
        setCode(req, "USERONLY");
        setOrderAmount(req, new BigDecimal("200.00"));

        mockMvc.perform(post("/api/coupons/validate")
                        .header("X-User-Id", "99")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    // -- setActive -------------------------------------------------------------

    @Test
    void setActive_adminRole_togglesState() throws Exception {
        Long id = createCouponAndGetId("TOGGLE");

        mockMvc.perform(patch("/api/coupons/{id}/active", id)
                        .header("X-User-Role", "ADMIN")
                        .param("value", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.active").value(false));
    }

    // -- helpers ---------------------------------------------------------------

    private void createCoupon(String code) throws Exception {
        mockMvc.perform(post("/api/coupons")
                        .header("X-User-Role", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildCreateRequest(code))))
                .andExpect(status().isOk());
    }

    private Long createCouponAndGetId(String code) throws Exception {
        String body = mockMvc.perform(post("/api/coupons")
                        .header("X-User-Role", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildCreateRequest(code))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return objectMapper.readTree(body).path("data").path("id").longValue();
    }

    private void claimCoupon(String code, Long userId) throws Exception {
        ClaimCouponRequest req = new ClaimCouponRequest();
        setCode(req, code);
        mockMvc.perform(post("/api/coupons/claim")
                        .header("X-User-Id", userId.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());
    }

    private CreateCouponRequest buildCreateRequest(String code) {
        return buildCreateRequestOfType(code, CouponType.GLOBAL);
    }

    private CreateCouponRequest buildCreateRequestOfType(String code, CouponType type) {
        CreateCouponRequest req = new CreateCouponRequest();
        setField(req, "code", code);
        setField(req, "couponType", type);
        setField(req, "discountRate", new BigDecimal("10.00"));
        setField(req, "minOrderAmount", BigDecimal.ZERO);
        setField(req, "startAt", LocalDateTime.now().minusDays(1));
        setField(req, "endAt", LocalDateTime.now().plusDays(30));
        setField(req, "active", true);
        return req;
    }

    private void createCouponOfType(String code, CouponType type) throws Exception {
        mockMvc.perform(post("/api/coupons")
                        .header("X-User-Role", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(buildCreateRequestOfType(code, type))))
                .andExpect(status().isOk());
    }

    private void setField(Object obj, String fieldName, Object value) {
        try {
            var field = obj.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(obj, value);
        } catch (Exception e) {
            throw new RuntimeException("Could not set field " + fieldName, e);
        }
    }

    private void setCode(ClaimCouponRequest req, String code) {
        setField(req, "code", code);
    }

    private void setCode(ValidateCouponRequest req, String code) {
        setField(req, "code", code);
    }

    private void setOrderAmount(ValidateCouponRequest req, BigDecimal amount) {
        setField(req, "orderAmount", amount);
    }
}
