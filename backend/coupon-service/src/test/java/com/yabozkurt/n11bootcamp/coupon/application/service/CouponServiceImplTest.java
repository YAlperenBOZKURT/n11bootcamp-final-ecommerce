package com.yabozkurt.n11bootcamp.coupon.application.service;

import com.yabozkurt.n11bootcamp.coupon.application.service.impl.CouponServiceImpl;
import com.yabozkurt.n11bootcamp.coupon.domain.exception.CouponValidationException;
import com.yabozkurt.n11bootcamp.coupon.domain.model.Coupon;
import com.yabozkurt.n11bootcamp.coupon.domain.model.CouponClaim;
import com.yabozkurt.n11bootcamp.coupon.domain.model.CouponUse;
import com.yabozkurt.n11bootcamp.coupon.domain.model.enums.CouponType;
import com.yabozkurt.n11bootcamp.coupon.domain.repository.CouponClaimRepository;
import com.yabozkurt.n11bootcamp.coupon.domain.repository.CouponRepository;
import com.yabozkurt.n11bootcamp.coupon.domain.repository.CouponUseRepository;
import com.yabozkurt.n11bootcamp.coupon.presentation.dto.request.UseCouponRequest;
import com.yabozkurt.n11bootcamp.coupon.presentation.dto.request.ValidateCouponRequest;
import com.yabozkurt.n11bootcamp.coupon.presentation.dto.response.CouponValidationResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CouponServiceImplTest {

    @Mock private CouponRepository couponRepository;
    @Mock private CouponClaimRepository claimRepository;
    @Mock private CouponUseRepository useRepository;

    private CouponServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new CouponServiceImpl(couponRepository, claimRepository, useRepository);
    }

    @Test
    void validate_shouldFail_whenBelowMinOrderAmount() {
        Coupon coupon = activeCoupon(CouponType.GLOBAL, 20, 500);
        when(couponRepository.findByCodeIgnoreCase("WELCOME20")).thenReturn(Optional.of(coupon));
        when(claimRepository.findByCouponIdAndUserId(10L, 99L)).thenReturn(Optional.empty());

        ValidateCouponRequest req = new ValidateCouponRequest();
        setField(req, "code", "WELCOME20");
        setField(req, "orderAmount", BigDecimal.valueOf(300));

        assertThrows(CouponValidationException.class, () -> service.validate(99L, req));
    }

    @Test
    void validate_shouldFail_whenUserCouponNotClaimed() {
        Coupon coupon = activeCoupon(CouponType.USER, 10, 100);
        when(couponRepository.findByCodeIgnoreCase("USER10")).thenReturn(Optional.of(coupon));
        when(claimRepository.findByCouponIdAndUserId(10L, 77L)).thenReturn(Optional.empty());

        ValidateCouponRequest req = new ValidateCouponRequest();
        setField(req, "code", "USER10");
        setField(req, "orderAmount", BigDecimal.valueOf(1000));

        assertThrows(CouponValidationException.class, () -> service.validate(77L, req));
    }

    @Test
    void use_shouldThrow_alreadyUsedMessage_whenSingleUseCouponAlreadyUsed() {
        Coupon coupon = activeCoupon(CouponType.GLOBAL, 10, 100);
        coupon.setPerUserLimit(1);
        when(couponRepository.findByCodeIgnoreCaseForUpdate("GLOBAL10")).thenReturn(Optional.of(coupon));
        when(useRepository.findByOrderIdAndCouponIdAndUserId("ORD-2", 10L, 5L)).thenReturn(Optional.empty());

        CouponClaim claim = new CouponClaim();
        claim.setCoupon(coupon);
        claim.setUserId(5L);
        claim.setUsageCount(1);
        when(claimRepository.findByCouponIdAndUserIdForUpdate(10L, 5L)).thenReturn(Optional.of(claim));

        UseCouponRequest req = new UseCouponRequest();
        setField(req, "orderId", "ORD-2");
        setField(req, "code", "GLOBAL10");
        setField(req, "orderAmount", BigDecimal.valueOf(200));

        CouponValidationException ex = assertThrows(CouponValidationException.class,
                () -> service.use(5L, req));
        assertEquals("Bu kuponu daha önce kullandınız", ex.getMessage());
    }

    @Test
    void use_shouldThrow_limitReachedMessage_whenMultiUseCouponExhausted() {
        Coupon coupon = activeCoupon(CouponType.GLOBAL, 10, 100);
        coupon.setPerUserLimit(3);
        when(couponRepository.findByCodeIgnoreCaseForUpdate("GLOBAL10")).thenReturn(Optional.of(coupon));
        when(useRepository.findByOrderIdAndCouponIdAndUserId("ORD-4", 10L, 5L)).thenReturn(Optional.empty());

        CouponClaim claim = new CouponClaim();
        claim.setCoupon(coupon);
        claim.setUserId(5L);
        claim.setUsageCount(3);
        when(claimRepository.findByCouponIdAndUserIdForUpdate(10L, 5L)).thenReturn(Optional.of(claim));

        UseCouponRequest req = new UseCouponRequest();
        setField(req, "orderId", "ORD-4");
        setField(req, "code", "GLOBAL10");
        setField(req, "orderAmount", BigDecimal.valueOf(200));

        CouponValidationException ex = assertThrows(CouponValidationException.class,
                () -> service.use(5L, req));
        assertEquals("Bu kupon için kullanım limitinize ulaştınız", ex.getMessage());
    }

    @Test
    void use_shouldBeIdempotent_whenSameOrderIdComesAgain() {
        Coupon coupon = activeCoupon(CouponType.GLOBAL, 10, 100);
        when(couponRepository.findByCodeIgnoreCaseForUpdate("GLOBAL10")).thenReturn(Optional.of(coupon));
        when(useRepository.findByOrderIdAndCouponIdAndUserId("ORD-1", 10L, 5L))
                .thenReturn(Optional.of(existingUse(coupon, 5L, "ORD-1", 120, 108)));

        UseCouponRequest req = new UseCouponRequest();
        setField(req, "orderId", "ORD-1");
        setField(req, "code", "GLOBAL10");
        setField(req, "orderAmount", BigDecimal.valueOf(120));

        CouponValidationResponse result = service.use(5L, req);

        assertEquals(0, BigDecimal.valueOf(12).compareTo(result.getDiscountAmount()));
        assertEquals(0, BigDecimal.valueOf(108).compareTo(result.getFinalAmount()));
        verify(claimRepository, never()).save(any());
    }

    private static Coupon activeCoupon(CouponType type, int discountRate, int minOrderAmount) {
        Coupon coupon = new Coupon();
        setField(coupon, "id", 10L);
        coupon.setCode(type == CouponType.GLOBAL ? "GLOBAL10" : "USER10");
        coupon.setCouponType(type);
        coupon.setDiscountRate(BigDecimal.valueOf(discountRate));
        coupon.setMinOrderAmount(BigDecimal.valueOf(minOrderAmount));
        coupon.setStartAt(LocalDateTime.now().minusDays(1));
        coupon.setEndAt(LocalDateTime.now().plusDays(1));
        coupon.setActive(true);
        return coupon;
    }

    private static CouponUse existingUse(Coupon coupon, Long userId, String orderId, int discount, int finalAmount) {
        CouponUse use = new CouponUse();
        use.setCoupon(coupon);
        use.setUserId(userId);
        use.setOrderId(orderId);
        use.setDiscountAmount(BigDecimal.valueOf(discount).movePointLeft(1));
        use.setFinalAmount(BigDecimal.valueOf(finalAmount));
        return use;
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
