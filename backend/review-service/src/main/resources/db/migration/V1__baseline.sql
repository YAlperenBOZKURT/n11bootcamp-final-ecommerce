CREATE TABLE reviews (
    id           BIGSERIAL PRIMARY KEY,
    product_id   BIGINT NOT NULL,
    user_id      BIGINT NOT NULL,
    rating       INT NOT NULL,
    comment_text VARCHAR(2000) NOT NULL,
    status       VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at   TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT chk_reviews_rating CHECK (rating >= 1 AND rating <= 5)
);

CREATE INDEX idx_reviews_product_id ON reviews(product_id);
CREATE INDEX idx_reviews_user_id ON reviews(user_id);
CREATE INDEX idx_reviews_status ON reviews(status);

