CREATE TABLE coupons (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(64) NOT NULL UNIQUE,
    coupon_type VARCHAR(16) NOT NULL,
    discount_rate NUMERIC(5,2) NOT NULL,
    min_order_amount NUMERIC(12,2) NOT NULL,
    start_at TIMESTAMP NOT NULL,
    end_at TIMESTAMP NOT NULL,
    per_user_limit INTEGER NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_discount_rate_range CHECK (discount_rate > 0 AND discount_rate <= 100),
    CONSTRAINT chk_per_user_limit_positive CHECK (per_user_limit IS NULL OR per_user_limit > 0),
    CONSTRAINT chk_coupon_type CHECK (coupon_type IN ('GLOBAL', 'USER'))
);

CREATE TABLE coupon_users (
    id BIGSERIAL PRIMARY KEY,
    coupon_id BIGINT NOT NULL REFERENCES coupons(id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL,
    usage_count INTEGER NOT NULL DEFAULT 0,
    claimed_at TIMESTAMP NOT NULL DEFAULT NOW(),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_coupon_user UNIQUE (coupon_id, user_id),
    CONSTRAINT chk_usage_count_non_negative CHECK (usage_count >= 0)
);

CREATE INDEX idx_coupon_users_coupon_id ON coupon_users(coupon_id);
CREATE INDEX idx_coupon_users_user_id ON coupon_users(user_id);

CREATE TABLE coupon_uses (
    id BIGSERIAL PRIMARY KEY,
    coupon_id BIGINT NOT NULL REFERENCES coupons(id) ON DELETE CASCADE,
    user_id BIGINT NOT NULL,
    order_id VARCHAR(100) NOT NULL,
    discount_amount NUMERIC(12,2) NOT NULL,
    final_amount NUMERIC(12,2) NOT NULL,
    used_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_coupon_use_order_coupon_user UNIQUE (order_id, coupon_id, user_id)
);

CREATE INDEX idx_coupon_uses_coupon_id ON coupon_uses(coupon_id);
CREATE INDEX idx_coupon_uses_user_id ON coupon_uses(user_id);