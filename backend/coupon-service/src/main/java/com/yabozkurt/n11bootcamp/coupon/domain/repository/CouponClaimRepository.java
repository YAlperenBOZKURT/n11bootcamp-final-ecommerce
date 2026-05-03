package com.yabozkurt.n11bootcamp.coupon.domain.repository;

import com.yabozkurt.n11bootcamp.coupon.domain.model.CouponClaim;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import jakarta.persistence.LockModeType;

import java.util.List;
import java.util.Optional;

public interface CouponClaimRepository extends JpaRepository<CouponClaim, Long> {
    Optional<CouponClaim> findByCouponIdAndUserId(Long couponId, Long userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from CouponClaim c where c.coupon.id = :couponId and c.userId = :userId")
    Optional<CouponClaim> findByCouponIdAndUserIdForUpdate(Long couponId, Long userId);

    @EntityGraph(attributePaths = "coupon")
    List<CouponClaim> findByUserId(Long userId);
}
