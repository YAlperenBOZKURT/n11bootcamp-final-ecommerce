-- ─── SCHEMA ───────────────────────────────────────────────────────────────────

CREATE TABLE stocks (
    id                BIGSERIAL PRIMARY KEY,
    product_id        BIGINT NOT NULL,
    variant_id        BIGINT,
    quantity          INT NOT NULL DEFAULT 0,
    reserved_quantity INT NOT NULL DEFAULT 0,
    created_at        TIMESTAMP DEFAULT NOW(),
    updated_at        TIMESTAMP DEFAULT NOW(),
    CONSTRAINT chk_quantity  CHECK (quantity >= 0),
    CONSTRAINT chk_reserved  CHECK (reserved_quantity >= 0),
    CONSTRAINT chk_available CHECK (quantity >= reserved_quantity),
    CONSTRAINT uq_stock_product_variant UNIQUE (product_id, variant_id)
);

CREATE INDEX idx_stocks_product_id ON stocks(product_id);
CREATE INDEX idx_stocks_variant_id ON stocks(variant_id);

CREATE TABLE stock_movements (
    id         BIGSERIAL PRIMARY KEY,
    stock_id   BIGINT NOT NULL REFERENCES stocks(id) ON DELETE CASCADE,
    type       VARCHAR(20) NOT NULL,
    quantity   INT NOT NULL,
    note       VARCHAR(255),
    created_at TIMESTAMP DEFAULT NOW()
);

CREATE INDEX idx_movements_stock_id ON stock_movements(stock_id);
CREATE INDEX idx_movements_type     ON stock_movements(type);

-- ─── SEED: Variant bazlı stoklar ─────────────────────────────────────────────
-- Adım 1: product-service V1'deki product -> variant id aralıklarını birebir kullan.
-- Adım 2: Her aralığı generate_series ile tek tek variant satırlarına aç.
-- Adım 3: Her variant için başlangıç stoku oluştur (quantity=30, reserved=0).
-- Adım 4: Product-level stok seedleme YOK; bu servis artık yalnızca variant bazlı çalışır.
-- Sonuç: Ürün düzenleme ekranında varyant stokları dolu gelir ve ödeme rezerv akışı 404'e düşmez.
INSERT INTO stocks (product_id, variant_id, quantity, reserved_quantity)
SELECT v.product_id, gs.variant_id, 30, 0
FROM (
    VALUES
    (1,1,3),(2,4,6),(3,7,9),(4,10,13),(5,14,16),(6,17,20),(7,21,24),(8,25,29),
    (9,30,33),(10,34,37),(11,38,41),(12,42,45),(13,46,49),(14,50,53),(15,54,57),
    (16,58,61),(17,62,65),(18,66,69),(19,70,73),(20,74,77),(21,78,80),(22,81,84),
    (23,85,87),(24,88,90),(25,91,92),(26,93,95),(27,96,98),(28,99,101),(29,102,106),
    (30,107,112),(31,113,117),(32,118,123),(33,124,128),(34,129,134),(35,135,140),
    (36,141,148),(37,149,152),(38,153,156),(39,157,160),(40,161,163),(41,164,167),
    (42,168,170),(43,171,172),(44,173,174),(45,175,175),(46,176,178)
) AS v(product_id, min_variant_id, max_variant_id)
CROSS JOIN LATERAL generate_series(v.min_variant_id, v.max_variant_id) AS gs(variant_id);
