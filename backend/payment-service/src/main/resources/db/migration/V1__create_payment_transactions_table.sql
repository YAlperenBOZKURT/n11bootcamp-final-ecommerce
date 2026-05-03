CREATE TABLE payment_transactions (
    id BIGSERIAL PRIMARY KEY,
    order_id VARCHAR(100) NOT NULL UNIQUE,
    user_id BIGINT NOT NULL,
    amount NUMERIC(12,2) NOT NULL,
    currency VARCHAR(8) NOT NULL,
    status VARCHAR(20) NOT NULL,
    provider VARCHAR(100),
    provider_payment_id VARCHAR(100),
    fail_reason VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_payment_transactions_user_id ON payment_transactions(user_id);
