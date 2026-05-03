WITH seed_ts AS (
    SELECT NOW() AS base_ts
)
INSERT INTO coupons (
    code,
    coupon_type,
    discount_rate,
    min_order_amount,
    start_at,
    end_at,
    per_user_limit,
    active,
    created_at,
    updated_at
)
SELECT 'n11-20', 'GLOBAL', 20.00, 500.00, base_ts, base_ts + INTERVAL '1 month', 1, TRUE, base_ts, base_ts FROM seed_ts
UNION ALL
SELECT 'n11-15', 'GLOBAL', 15.00, 800.00, base_ts, base_ts + INTERVAL '1 month', 1, TRUE, base_ts, base_ts FROM seed_ts
UNION ALL
SELECT 'n11-10', 'GLOBAL', 10.00, 1500.00, base_ts, base_ts + INTERVAL '1 month', 1, TRUE, base_ts, base_ts FROM seed_ts;
