CREATE TABLE order_items (
    id           BIGSERIAL PRIMARY KEY,
    order_id     BIGINT NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    product_id   BIGINT NOT NULL,
    variant_id   BIGINT,
    product_name VARCHAR(255) NOT NULL,
    unit_price   NUMERIC(12, 2) NOT NULL,
    quantity     INT NOT NULL,
    line_total   NUMERIC(12, 2) NOT NULL
);

CREATE INDEX idx_order_items_order_id ON order_items(order_id);
