package com.yabozkurt.n11bootcamp.coupon.application.service.impl;

import com.yabozkurt.n11bootcamp.coupon.application.service.CouponService;
import com.yabozkurt.n11bootcamp.coupon.domain.exception.CouponNotFoundException;
import com.yabozkurt.n11bootcamp.coupon.domain.exception.CouponValidationException;
import com.yabozkurt.n11bootcamp.coupon.domain.model.Coupon;
import com.yabozkurt.n11bootcamp.coupon.domain.model.CouponClaim;
import com.yabozkurt.n11bootcamp.coupon.domain.model.CouponUse;
import com.yabozkurt.n11bootcamp.coupon.domain.model.enums.CouponType;
import com.yabozkurt.n11bootcamp.coupon.domain.repository.CouponClaimRepository;
import com.yabozkurt.n11bootcamp.coupon.domain.repository.CouponRepository;
import com.yabozkurt.n11bootcamp.coupon.domain.repository.CouponUseRepository;
import com.yabozkurt.n11bootcamp.coupon.presentation.dto.request.*;
import com.yabozkurt.n11bootcamp.coupon.presentation.dto.response.CouponResponse;
import com.yabozkurt.n11bootcamp.coupon.presentation.dto.response.CouponValidationResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

@Service
@Transactional(readOnly = true)
public class CouponServiceImpl implements CouponService {

    private static final Logger log = LoggerFactory.getLogger(CouponServiceImpl.class);

    private final CouponRepository couponRepository;
    private final CouponClaimRepository claimRepository;
    private final CouponUseRepository useRepository;

    public CouponServiceImpl(CouponRepository couponRepository, CouponClaimRepository claimRepository, CouponUseRepository useRepository) {
        this.couponRepository = couponRepository;
        this.claimRepository = claimRepository;
        this.useRepository = useRepository;
    }

    @Override
    @Transactional
    public CouponResponse create(CreateCouponRequest request) {
        String code = normalizeCode(request.getCode());
        if (couponRepository.existsByCodeIgnoreCase(code)) {
            throw new CouponValidationException("Bu kupon kodu zaten mevcut");
        }
        validateDateWindow(request.getStartAt(), request.getEndAt());

        Coupon coupon = new Coupon();
        coupon.setCode(code);
        coupon.setCouponType(request.getCouponType());
        coupon.setDiscountRate(request.getDiscountRate());
        coupon.setMinOrderAmount(request.getMinOrderAmount());
        coupon.setStartAt(request.getStartAt());
        coupon.setEndAt(request.getEndAt());
        coupon.setPerUserLimit(request.getPerUserLimit());
        coupon.setActive(Boolean.TRUE.equals(request.getActive()));

        CouponResponse response = toResponse(couponRepository.save(coupon));
        log.info("Coupon created: code={}, type={}, discountRate={}", code, coupon.getCouponType(), coupon.getDiscountRate());
        return response;
    }

    @Override
    @Transactional
    public CouponResponse update(Long id, UpdateCouponRequest request) {
        Coupon coupon = findById(id);
        validateDateWindow(request.getStartAt(), request.getEndAt());

        coupon.setCouponType(request.getCouponType());
        coupon.setDiscountRate(request.getDiscountRate());
        coupon.setMinOrderAmount(request.getMinOrderAmount());
        coupon.setStartAt(request.getStartAt());
        coupon.setEndAt(request.getEndAt());
        coupon.setPerUserLimit(request.getPerUserLimit());
        coupon.setActive(request.getActive());

        return toResponse(couponRepository.save(coupon));
    }

    @Override
    public CouponResponse getById(Long id) {
        return toResponse(findById(id));
    }

    @Override
    public Page<CouponResponse> getAll(Boolean active, CouponType type, String code, Pageable pageable) {
        Specification<Coupon> spec = Specification.where(null);
        if (active != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("active"), active));
        }
        if (type != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("couponType"), type));
        }
        if (code != null && !code.isBlank()) {
            String q = "%" + code.toLowerCase(Locale.ROOT) + "%";
            spec = spec.and((root, query, cb) -> cb.like(cb.lower(root.get("code")), q));
        }
        return couponRepository.findAll(spec, pageable).map(CouponServiceImpl::toResponse);
    }

    @Override
    public List<CouponResponse> getMyCoupons(Long userId) {
        return claimRepository.findByUserId(userId).stream()
                .map(CouponClaim::getCoupon)
                .map(CouponServiceImpl::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public CouponResponse setActive(Long id, boolean active) {
        Coupon coupon = findById(id);
        coupon.setActive(active);
        return toResponse(couponRepository.save(coupon));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        Coupon coupon = findById(id);
        coupon.setActive(false);
        couponRepository.save(coupon);
    }

    @Override
    @Transactional
    public void claim(Long userId, ClaimCouponRequest request) {
        Coupon coupon = findByCode(request.getCode());
        if (coupon.getCouponType() != CouponType.USER) {
            throw new CouponValidationException("Sadece USER tipindeki kuponlar hesaba eklenebilir");
        }
        validateStateAndDate(coupon);

        claimRepository.findByCouponIdAndUserId(coupon.getId(), userId)
                .ifPresent(existing -> {
                    throw new CouponValidationException("Kupon zaten bu kullanıcıya eklenmiş");
                });

        CouponClaim claim = new CouponClaim();
        claim.setCoupon(coupon);
        claim.setUserId(userId);
        claimRepository.save(claim);
        log.info("Coupon claimed: code={}, userId={}", coupon.getCode(), userId);
    }

    @Override
    public CouponValidationResponse validate(Long userId, ValidateCouponRequest request) {
        Coupon coupon = findByCode(request.getCode());
        CouponClaim claim = claimRepository.findByCouponIdAndUserId(coupon.getId(), userId).orElse(null);
        return validateInternal(coupon, userId, request.getOrderAmount(), claim);
    }

    @Override
    @Transactional
    public CouponValidationResponse use(Long userId, UseCouponRequest request) {
        Coupon coupon = findByCodeForUpdate(request.getCode());

        CouponUse existingUse = useRepository.findByOrderIdAndCouponIdAndUserId(request.getOrderId(), coupon.getId(), userId).orElse(null);
        if (existingUse != null) {
            return toValidationResponse(coupon, existingUse.getDiscountAmount(), existingUse.getFinalAmount());
        }

        CouponClaim claim = claimRepository.findByCouponIdAndUserIdForUpdate(coupon.getId(), userId).orElse(null);
        CouponValidationResponse result = validateInternal(coupon, userId, request.getOrderAmount(), claim);

        if (claim == null) {
            claim = new CouponClaim();
            claim.setCoupon(coupon);
            claim.setUserId(userId);
            claim.setUsageCount(0);
            try {
                claim = claimRepository.save(claim);
            } catch (DataIntegrityViolationException ignored) {
                claim = claimRepository.findByCouponIdAndUserIdForUpdate(coupon.getId(), userId)
                        .orElseThrow(() -> new CouponValidationException("Kupon kullanım kaydı oluşturulamadı"));
            }
        }
        claim.setUsageCount(claim.getUsageCount() + 1);
        claimRepository.save(claim);

        CouponUse couponUse = new CouponUse();
        couponUse.setCoupon(coupon);
        couponUse.setUserId(userId);
        couponUse.setOrderId(request.getOrderId());
        couponUse.setDiscountAmount(result.getDiscountAmount());
        couponUse.setFinalAmount(result.getFinalAmount());
        try {
            useRepository.save(couponUse);
        } catch (DataIntegrityViolationException ignored) {
            CouponUse duplicated = useRepository.findByOrderIdAndCouponIdAndUserId(request.getOrderId(), coupon.getId(), userId)
                    .orElseThrow(() -> new CouponValidationException("Kupon kullanım kaydı okunamadı"));
            return toValidationResponse(coupon, duplicated.getDiscountAmount(), duplicated.getFinalAmount());
        }

        log.info("Coupon applied: code={}, userId={}, orderId={}, discount={}, finalAmount={}",
                coupon.getCode(), userId, request.getOrderId(), result.getDiscountAmount(), result.getFinalAmount());
        return result;
    }

    private CouponValidationResponse validateInternal(Coupon coupon, Long userId, BigDecimal orderAmount, CouponClaim claim) {
        validateStateAndDate(coupon);

        if (orderAmount.compareTo(coupon.getMinOrderAmount()) < 0) {
            throw new CouponValidationException("Sepet tutarı minimum sipariş tutarını karşılamıyor");
        }

        if (coupon.getCouponType() == CouponType.USER && claim == null) {
            throw new CouponValidationException("Bu kupon kullanıcı hesabına eklenmemiş");
        }

        int usageCount = claim == null ? 0 : claim.getUsageCount();
        if (coupon.getPerUserLimit() != null && usageCount >= coupon.getPerUserLimit()) {
            throw new CouponValidationException("Kullanıcı kupon kullanım limitine ulaştı");
        }

        BigDecimal discountAmount = orderAmount
                .multiply(coupon.getDiscountRate())
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
        BigDecimal finalAmount = orderAmount.subtract(discountAmount).max(BigDecimal.ZERO);

        CouponValidationResponse response = new CouponValidationResponse();
        response.setCouponId(coupon.getId());
        response.setCode(coupon.getCode());
        response.setDiscountAmount(discountAmount);
        response.setFinalAmount(finalAmount);
        return response;
    }

    private Coupon findById(Long id) {
        return couponRepository.findById(id).orElseThrow(() -> new CouponNotFoundException(id));
    }

    private Coupon findByCode(String code) {
        return couponRepository.findByCodeIgnoreCase(normalizeCode(code))
                .orElseThrow(() -> new CouponNotFoundException(code));
    }

    private Coupon findByCodeForUpdate(String code) {
        return couponRepository.findByCodeIgnoreCaseForUpdate(normalizeCode(code))
                .orElseThrow(() -> new CouponNotFoundException(code));
    }

    private static String normalizeCode(String code) {
        return code == null ? null : code.trim().toUpperCase(Locale.ROOT);
    }

    private static void validateDateWindow(LocalDateTime startAt, LocalDateTime endAt) {
        if (!endAt.isAfter(startAt)) {
            throw new CouponValidationException("endAt, startAt tarihinden sonra olmalı");
        }
    }

    private static void validateStateAndDate(Coupon coupon) {
        if (!coupon.isActive()) {
            throw new CouponValidationException("Kupon aktif değil");
        }
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(coupon.getStartAt())) {
            throw new CouponValidationException("Kupon henüz aktifleşmedi");
        }
        if (now.isAfter(coupon.getEndAt())) {
            throw new CouponValidationException("Kuponun süresi dolmuş");
        }
    }

    public static CouponResponse toResponse(Coupon coupon) {
        CouponResponse response = new CouponResponse();
        response.setId(coupon.getId());
        response.setCode(coupon.getCode());
        response.setCouponType(coupon.getCouponType());
        response.setDiscountRate(coupon.getDiscountRate());
        response.setMinOrderAmount(coupon.getMinOrderAmount());
        response.setStartAt(coupon.getStartAt());
        response.setEndAt(coupon.getEndAt());
        response.setPerUserLimit(coupon.getPerUserLimit());
        response.setActive(coupon.isActive());
        response.setCreatedAt(coupon.getCreatedAt());
        response.setUpdatedAt(coupon.getUpdatedAt());
        return response;
    }

    private static CouponValidationResponse toValidationResponse(Coupon coupon, BigDecimal discountAmount, BigDecimal finalAmount) {
        CouponValidationResponse response = new CouponValidationResponse();
        response.setCouponId(coupon.getId());
        response.setCode(coupon.getCode());
        response.setDiscountAmount(discountAmount);
        response.setFinalAmount(finalAmount);
        return response;
    }
}
