package com.yabozkurt.n11bootcamp.coupon.domain.repository;

import com.yabozkurt.n11bootcamp.coupon.domain.model.CouponUse;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CouponUseRepository extends JpaRepository<CouponUse, Long> {
    Optional<CouponUse> findByOrderIdAndCouponIdAndUserId(String orderId, Long couponId, Long userId);
}
