package com.yabozkurt.n11bootcamp.coupon.domain.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "coupon_users",
        uniqueConstraints = @UniqueConstraint(name = "uk_coupon_user", columnNames = {"coupon_id", "user_id"})
)
public class CouponClaim {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "coupon_id", nullable = false)
    private Coupon coupon;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(nullable = false)
    private Integer usageCount = 0;

    @Column(nullable = false)
    private LocalDateTime claimedAt;

    @Version
    private Long version;

    @PrePersist
    void onCreate() {
        if (claimedAt == null) {
            claimedAt = LocalDateTime.now();
        }
    }

    public Long getId() {
        return id;
    }

    public Coupon getCoupon() {
        return coupon;
    }

    public void setCoupon(Coupon coupon) {
        this.coupon = coupon;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Integer getUsageCount() {
        return usageCount;
    }

    public void setUsageCount(Integer usageCount) {
        this.usageCount = usageCount;
    }

    public LocalDateTime getClaimedAt() {
        return claimedAt;
    }
}
