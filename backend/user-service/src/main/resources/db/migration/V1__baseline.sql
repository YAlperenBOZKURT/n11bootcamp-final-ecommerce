-- ─── SCHEMA ───────────────────────────────────────────────────────────────────

CREATE TABLE users (
    id           BIGSERIAL PRIMARY KEY,
    email        VARCHAR(255) NOT NULL UNIQUE,
    password     VARCHAR(255) NOT NULL,
    first_name   VARCHAR(100) NOT NULL,
    last_name    VARCHAR(100) NOT NULL,
    phone_number VARCHAR(20) UNIQUE,
    role         VARCHAR(20)  NOT NULL DEFAULT 'CUSTOMER',
    status       VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',
    created_at   TIMESTAMP DEFAULT NOW(),
    updated_at   TIMESTAMP DEFAULT NOW()
);

CREATE TABLE addresses (
    id               BIGSERIAL PRIMARY KEY,
    user_id          BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    title            VARCHAR(100) NOT NULL,
    recipient_name   VARCHAR(150) NOT NULL,
    recipient_phone  VARCHAR(20) NOT NULL,
    city             VARCHAR(100) NOT NULL,
    district         VARCHAR(100) NOT NULL,
    neighborhood     VARCHAR(150),
    address_line     TEXT NOT NULL,
    zip_code         VARCHAR(10),
    is_default       BOOLEAN NOT NULL DEFAULT FALSE,
    created_at       TIMESTAMP DEFAULT NOW(),
    updated_at       TIMESTAMP DEFAULT NOW()
);

CREATE INDEX idx_addresses_user_id ON addresses(user_id);

-- ─── SEED: Admin kullanıcısı ──────────────────────────────────────────────────
-- E-posta : admin@bozkurt.com
-- Şifre   : Admin123!   (BCrypt $2b$10)
INSERT INTO users (email, password, first_name, last_name, role, status)
VALUES (
    'admin@bozkurt.com',
    '$2a$10$JzhIF0ptNSx4FOz/ckOQNe2uS61.xyrhC5y5kej8iVhTJ.j2db17e',
    'Admin',
    'Bozkurt',
    'ADMIN',
    'ACTIVE'
)
ON CONFLICT (email) DO NOTHING;
