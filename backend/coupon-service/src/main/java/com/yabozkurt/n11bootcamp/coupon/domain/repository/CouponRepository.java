package com.yabozkurt.n11bootcamp.coupon.domain.repository;

import com.yabozkurt.n11bootcamp.coupon.domain.model.Coupon;
import com.yabozkurt.n11bootcamp.coupon.domain.model.enums.CouponType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import jakarta.persistence.LockModeType;

import java.util.Optional;

public interface CouponRepository extends JpaRepository<Coupon, Long>, JpaSpecificationExecutor<Coupon> {
    Optional<Coupon> findByCodeIgnoreCase(String code);
    boolean existsByCodeIgnoreCase(String code);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from Coupon c where lower(c.code) = lower(:code)")
    Optional<Coupon> findByCodeIgnoreCaseForUpdate(String code);
}
