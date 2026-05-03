CREATE TABLE orders (
    id              BIGSERIAL PRIMARY KEY,
    order_number    VARCHAR(64) NOT NULL UNIQUE,
    user_id         BIGINT NOT NULL,
    user_email      VARCHAR(255),
    status          VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    total_amount    NUMERIC(12, 2) NOT NULL,
    discount_amount NUMERIC(12, 2) NOT NULL DEFAULT 0,
    final_amount    NUMERIC(12, 2) NOT NULL,
    coupon_code     VARCHAR(64),
    fail_reason     VARCHAR(500),
    created_at      TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_orders_user_id ON orders(user_id);
CREATE INDEX idx_orders_status  ON orders(status);
