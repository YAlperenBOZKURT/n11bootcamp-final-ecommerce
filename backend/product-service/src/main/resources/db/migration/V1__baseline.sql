-- ─── SCHEMA ───────────────────────────────────────────────────────────────────

CREATE TABLE categories (
    id         BIGSERIAL PRIMARY KEY,
    name       VARCHAR(100) NOT NULL UNIQUE,
    parent_id  BIGINT REFERENCES categories(id) ON DELETE SET NULL,
    created_at TIMESTAMP DEFAULT NOW(),
    updated_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE products (
    id          BIGSERIAL PRIMARY KEY,
    category_id BIGINT REFERENCES categories(id) ON DELETE SET NULL,
    name        VARCHAR(255) NOT NULL,
    description TEXT,
    brand       VARCHAR(100),
    image_urls  JSONB DEFAULT '[]',
    status      VARCHAR(20) NOT NULL DEFAULT 'PASSIVE',
    created_at  TIMESTAMP DEFAULT NOW(),
    updated_at  TIMESTAMP DEFAULT NOW()
);

CREATE INDEX idx_products_category_id  ON products(category_id);
CREATE INDEX idx_products_status       ON products(status);

CREATE TABLE product_variants (
    id                 BIGSERIAL PRIMARY KEY,
    product_id         BIGINT NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    sku                VARCHAR(100) NOT NULL UNIQUE,
    attributes         JSONB DEFAULT '{}',
    price              NUMERIC(12,2) NOT NULL,
    original_price     NUMERIC(12,2),
    discount_rate      NUMERIC(5,2),
    discount_start_at  TIMESTAMP,
    discount_end_at    TIMESTAMP,
    status             VARCHAR(20) NOT NULL DEFAULT 'PASSIVE',
    created_at         TIMESTAMP DEFAULT NOW(),
    updated_at         TIMESTAMP DEFAULT NOW()
);

CREATE INDEX idx_variants_product_id  ON product_variants(product_id);
CREATE INDEX idx_variants_sku         ON product_variants(sku);
CREATE INDEX idx_variants_attributes  ON product_variants USING GIN(attributes);

-- ─── SEED: Kategoriler ────────────────────────────────────────────────────────

INSERT INTO categories (id, name, parent_id) VALUES
(1, 'Kadın Giyim',           NULL),
(2, 'Erkek Giyim',           NULL),
(3, 'Çocuk Giyim',           NULL),
(4, 'Telefon & Aksesuarlar', NULL),
(5, 'Ayakkabı',              NULL),
(6, 'Spor & Outdoor',        NULL),
(7, 'Bilgisayar & Tablet',   NULL);

SELECT setval('categories_id_seq', 7);

-- ─── SEED: Ürünler ────────────────────────────────────────────────────────────

INSERT INTO products (id, category_id, name, description, brand, image_urls, status) VALUES

-- ══ KADIN GİYİM ══════════════════════════════════════════════════════════════
(1,  1, 'Çiçekli Midi Elbise',         'Yazlık çiçek desenli midi elbise, viskon kumaş, şık ve rahat kesim.',      'Mavi',            '["https://images.unsplash.com/photo-1515372039744-b8f02a3ae446?w=400&q=80"]'::jsonb, 'ACTIVE'),
(2,  1, 'Basic Oversize Bluz',         'Günlük kullanım için oversize kesim, %100 pamuk kumaş.',                   'Koton',           '["https://images.unsplash.com/photo-1496747611176-843222e1e57c?w=400&q=80"]'::jsonb, 'ACTIVE'),
(3,  1, 'Yüksek Bel Skinny Jean',      'Yüksek bel, dar paça denim pantolon, esnek yapı.',                        'LC Waikiki',      '["https://images.unsplash.com/photo-1542295669297-4d352b042bca?w=400&q=80"]'::jsonb, 'ACTIVE'),
(4,  1, 'Baskılı Crop Top',            'Yazlık baskılı crop tişört, bel üstü kesim.',                             'DeFacto',         '["https://images.unsplash.com/photo-1544022613-e87ca75a784a?w=400&q=80"]'::jsonb, 'ACTIVE'),
(5,  1, 'Saten Gece Elbisesi',         'Özel günler için saten midi elbise, ince askılı kesim.',                  'Twist',           '["https://images.unsplash.com/photo-1572804013309-59a88b7e92f1?w=400&q=80"]'::jsonb, 'ACTIVE'),
(6,  1, 'Triko Kazak',                 'Kışlık slim fit triko kazak, yüksek yaka.',                               'Mango',           '["https://images.unsplash.com/photo-1485968579580-b6d095142e6e?w=400&q=80"]'::jsonb, 'ACTIVE'),
(7,  1, 'Pileli Mini Etek',            'Tenis etekten ilham alan pileli mini etek, yüksek bel.',                  'Zara',            '["https://images.unsplash.com/photo-1594938298603-c8148c4dae35?w=400&q=80"]'::jsonb, 'ACTIVE'),
(8,  1, 'Keten Gömlek',                'Yazlık oversize keten gömlek, düğmeli yaka.',                             'H&M',             '["https://images.unsplash.com/photo-1594938374182-a55e3b4f6d4c?w=400&q=80"]'::jsonb, 'ACTIVE'),

-- ══ ERKEK GİYİM ══════════════════════════════════════════════════════════════
(9,  2, 'Slim Fit Oxford Gömlek',      'Slim fit, düz renk oxford gömlek, iş ve günlük kullanıma uygun.',         'Mavi',            '["https://images.unsplash.com/photo-1620012253295-c15cc3e65df4?w=400&q=80"]'::jsonb, 'ACTIVE'),
(10, 2, 'Regular Fit Chino Pantolon',  'Her günün pantolonu, esnek kemer.',                                       'Koton',           '["https://images.unsplash.com/photo-1602810316498-ab67cf68c8e1?w=400&q=80"]'::jsonb, 'ACTIVE'),
(11, 2, 'Oversize Baskılı Tişört',     'Oversize grafik baskılı tişört, %100 pamuk.',                             'Pull&Bear',       '["https://images.unsplash.com/photo-1586790170083-2f9ceadc732d?w=400&q=80"]'::jsonb, 'ACTIVE'),
(12, 2, 'Slim Fit Blazer',             'Şık slim fit blazer ceket, keten karışımı.',                              'Zara Man',        '["https://images.unsplash.com/photo-1598033129183-c4f50c736f10?w=400&q=80"]'::jsonb, 'ACTIVE'),
(13, 2, 'Jogger Eşofman Altı',         'Günlük konforlu jogger, büzgülü paça.',                                   'Nike',            '["https://images.unsplash.com/photo-1611312449408-fcece27cdbb7?w=400&q=80"]'::jsonb, 'ACTIVE'),
(14, 2, 'Kapüşonlu Sweatshirt',        'Kapüşonlu, kanguru cepli sweatshirt, polar iç.',                         'DeFacto',         '["https://images.unsplash.com/photo-1503341504253-dff4815485f1?w=400&q=80"]'::jsonb, 'ACTIVE'),
(15, 2, 'Slim Fit Kot Pantolon',       'Slim fit, koyu indigo denim, 5 cepli.',                                   'Mavi',            '["https://images.unsplash.com/photo-1542272604-787c3835535d?w=400&q=80"]'::jsonb, 'ACTIVE'),
(16, 2, 'Rüzgarlık Mont',              'Hafif, su itici rüzgarlık, kapüşonlu.',                                   'Columbia',        '["https://images.unsplash.com/photo-1591047139829-d91aecb6caea?w=400&q=80"]'::jsonb, 'ACTIVE'),

-- ══ ÇOCUK GİYİM ══════════════════════════════════════════════════════════════
(17, 3, 'Çocuk Baskılı Pijama Takımı', 'Pamuklu yumuşak pijama takımı, sevimli baskı.',                          'LC Waikiki',      '["https://images.unsplash.com/photo-1519238263530-99bdd11df2ea?w=400&q=80"]'::jsonb, 'ACTIVE'),
(18, 3, 'Kız Çocuğu Fiyonklu Elbise', 'Özel günler için fiyonklu elbise, tül etek.',                             'Zara Kids',       '["https://images.unsplash.com/photo-1503944583220-79d8926ad5e2?w=400&q=80"]'::jsonb, 'ACTIVE'),
(19, 3, 'Erkek Çocuk Eşofman Takımı', 'İki parçalı pamuklu eşofman, baskılı.',                                   'H&M Kids',        '["https://images.unsplash.com/photo-1543807535-eceef0bc6599?w=400&q=80"]'::jsonb, 'ACTIVE'),
(20, 3, 'Bebek Tulum',                 'Organik pamuk bebek tulumu, metal çıtçıtlı.',                             'Mothercare',      '["https://images.unsplash.com/photo-1515488042361-ee00e0ddd4e4?w=400&q=80"]'::jsonb, 'ACTIVE'),

-- ══ TELEFON & AKSESUARLAR ════════════════════════════════════════════════════
(21, 4, 'Samsung Galaxy S24 256GB',    '6.2" Dynamic AMOLED, Snapdragon 8 Gen 3, 50MP kamera, Galaxy AI.',       'Samsung',         '["https://images.unsplash.com/photo-1511707171634-5f897ff02aa9?w=400&q=80"]'::jsonb, 'ACTIVE'),
(22, 4, 'Apple iPhone 15',             'A16 Bionic, 6.1" Super Retina XDR, 48MP kamera sistemi.',                'Apple',           '["https://images.unsplash.com/photo-1592750475338-74b7b21085ab?w=400&q=80"]'::jsonb, 'ACTIVE'),
(23, 4, 'Xiaomi Redmi Note 13 Pro',    'Snapdragon 7s Gen 2, 200MP kamera, 5000mAh pil.',                        'Xiaomi',          '["https://images.unsplash.com/photo-1601784551446-20c9e07cdbdb?w=400&q=80"]'::jsonb, 'ACTIVE'),
(24, 4, 'Apple iPhone 15 Pro Max',     'A17 Pro, 6.7" ProMotion, 5x optik zoom, titanyum çerçeve.',              'Apple',           '["https://images.unsplash.com/photo-1565849904461-04a58ad377e0?w=400&q=80"]'::jsonb, 'ACTIVE'),
(25, 4, 'Apple AirPods Pro 2. Nesil',  'Aktif gürültü engelleme, Adaptif Ses, USB-C şarj kutusu.',               'Apple',           '["https://images.unsplash.com/photo-1580910051074-3eb694886505?w=400&q=80"]'::jsonb, 'ACTIVE'),
(26, 4, 'Samsung Galaxy Tab S9 128GB', '11" AMOLED, Snapdragon 8 Gen 2, S Pen dahil, IP68.',                     'Samsung',         '["https://images.unsplash.com/photo-1551355738-1875b8c8c745?w=400&q=80"]'::jsonb, 'ACTIVE'),
(27, 4, 'Google Pixel 8',              'Google Tensor G3, 50MP kamera, 7 yıl güncelleme.',                       'Google',          '["https://images.unsplash.com/photo-1598327105854-d8f3c0f0c0bc?w=400&q=80"]'::jsonb, 'ACTIVE'),
(28, 4, 'Huawei Watch GT 4',           'Akıllı saat, 14 gün pil ömrü, sağlık takibi.',                           'Huawei',          '["https://images.unsplash.com/photo-1523275335684-37898b6baf30?w=400&q=80"]'::jsonb, 'ACTIVE'),

-- ══ AYAKKABI ═════════════════════════════════════════════════════════════════
(29, 5, 'Nike Air Max 270',            'Hafif koşu ve günlük kullanım, büyük Air birimi.',                        'Nike',            '["https://images.unsplash.com/photo-1542291026-7eec264c27ff?w=400&q=80"]'::jsonb, 'ACTIVE'),
(30, 5, 'Adidas Classic Leather Sneaker','Klasik deri spor ayakkabı, zamansız tasarım.',                          'Adidas',          '["https://images.unsplash.com/photo-1608231387042-66d1773070a5?w=400&q=80"]'::jsonb, 'ACTIVE'),
(31, 5, 'Yüksek Topuklu Stiletto',     'Sivri burunlu, 10cm topuklu, özel gün stiletti.',                        'Aldo',            '["https://images.unsplash.com/photo-1595950653106-6c9ebd614d3a?w=400&q=80"]'::jsonb, 'ACTIVE'),
(32, 5, 'Columbia Yürüyüş Botu',       'Su geçirmez, yüksek bilekli, Gore-Tex membran.',                         'Columbia',        '["https://images.unsplash.com/photo-1460353581641-37baddab0fa2?w=400&q=80"]'::jsonb, 'ACTIVE'),
(33, 5, 'Zara Platform Loafer',        'Kalın tabanlı, metal tokalı loafer, trend model.',                       'Zara',            '["https://images.unsplash.com/photo-1587563871167-1ee9c731aefb?w=400&q=80"]'::jsonb, 'ACTIVE'),
(34, 5, 'Nike Basketbol Ayakkabısı',   'Yüksek performanslı, yüksek bilekli basketbol ayakkabısı.',              'Nike',            '["https://images.unsplash.com/photo-1556906781-9a412961a28c?w=400&q=80"]'::jsonb, 'ACTIVE'),
(35, 5, 'Converse Chuck Taylor All Star','İkonik bez spor ayakkabı, kauçuk taban.',                              'Converse',        '["https://images.unsplash.com/photo-1562183241-b937e95585b6?w=400&q=80"]'::jsonb, 'ACTIVE'),
(36, 5, 'New Balance 574',             'Retro koşu stili, EVA taban, suede detaylar.',                           'New Balance',     '["https://images.unsplash.com/photo-1539185441755-769473a23570?w=400&q=80"]'::jsonb, 'ACTIVE'),

-- ══ SPOR & OUTDOOR ════════════════════════════════════════════════════════════
(37, 6, 'Decathlon Yoga Matı 6mm',     'Anti-slip TPE yüzey, 6mm koruma, taşıma askılı.',                        'Decathlon',       '["https://images.unsplash.com/photo-1571019613454-1cb2f99b2d8b?w=400&q=80"]'::jsonb, 'ACTIVE'),
(38, 6, 'Nutrend Protein Shaker 700ml','BPA free, sızdırmaz kapak, ölçüm skalası.',                              'Nutrend',         '["https://images.unsplash.com/photo-1579952363873-27f3bade9f55?w=400&q=80"]'::jsonb, 'ACTIVE'),
(39, 6, 'Outdoor Research Kayak Montu','Polar astar, rüzgar ve su geçirmez, kayak montu.',                       'Outdoor Research','["https://images.unsplash.com/photo-1520209759809-a9bcb6cb3241?w=400&q=80"]'::jsonb, 'ACTIVE'),
(40, 6, 'Garmin Forerunner 265 GPS',   'Koşu ve triatlon için GPS akıllı saat, AMOLED ekran.',                   'Garmin',          '["https://images.unsplash.com/photo-1508685096489-7aacd43bd3b1?w=400&q=80"]'::jsonb, 'ACTIVE'),

-- ══ BİLGİSAYAR & TABLET ══════════════════════════════════════════════════════
(41, 7, 'Apple MacBook Air M2 13"',    'Apple M2 çip, 13.6" Liquid Retina, 18 saat pil.',                        'Apple',           '["https://images.unsplash.com/photo-1517336714731-489689fd1ca8?w=400&q=80"]'::jsonb, 'ACTIVE'),
(42, 7, 'Apple MacBook Pro M3 Pro 14"','Apple M3 Pro çip, 14.2" Liquid Retina XDR.',                             'Apple',           '["https://images.unsplash.com/photo-1541807084-5c52b6b3adef?w=400&q=80"]'::jsonb, 'ACTIVE'),
(43, 7, 'Lenovo ThinkPad X1 Carbon Gen 11','Intel Core i7-1365U, 14" IPS, 1.12kg.',                             'Lenovo',          '["https://images.unsplash.com/photo-1496181133206-80ce9b88a853?w=400&q=80"]'::jsonb, 'ACTIVE'),
(44, 7, 'Asus ROG Strix G16 Gaming',   'Intel Core i9-13980HX, RTX 4070/4080, 32GB DDR5, 165Hz QHD.',            'Asus',            '["https://images.unsplash.com/photo-1593642632559-0c6d3fc62b89?w=400&q=80"]'::jsonb, 'ACTIVE'),
(45, 7, 'Dell UltraSharp 27" 4K Monitor','IPS panel, 3840x2160, USB-C 90W, sRGB %100.',                         'Dell',            '["https://images.unsplash.com/photo-1588872657578-7efd1f1555ed?w=400&q=80"]'::jsonb, 'ACTIVE'),
(46, 7, 'Logitech MX Master 3S Mouse', 'Sessiz tıklama, 8K DPI, ergonomik tasarım, çok cihaz desteği.',          'Logitech',        '["https://images.unsplash.com/photo-1525547719571-a2d4ac8945e2?w=400&q=80"]'::jsonb, 'ACTIVE');

SELECT setval('products_id_seq', 46);

-- ─── SEED: Varyantlar ─────────────────────────────────────────────────────────

INSERT INTO product_variants (product_id, sku, attributes, price, original_price, discount_rate, status) VALUES

-- ══ KADIN GİYİM ══════════════════════════════════════════════════════════════

-- Çiçekli Midi Elbise (1) — %35 indirim
(1, 'MDW-ELB01-S-BYZ', '{"beden":"S","renk":"Beyaz"}'::jsonb, 449.00, 699.00, 35, 'ACTIVE'),
(1, 'MDW-ELB01-M-BYZ', '{"beden":"M","renk":"Beyaz"}'::jsonb, 449.00, 699.00, 35, 'ACTIVE'),
(1, 'MDW-ELB01-L-LLA', '{"beden":"L","renk":"Lila"}'::jsonb,  449.00, 699.00, 35, 'ACTIVE'),

-- Basic Oversize Bluz (2) — %33 indirim
(2, 'MDW-BLZ02-S-SYH', '{"beden":"S","renk":"Siyah"}'::jsonb, 199.00, 299.00, 33, 'ACTIVE'),
(2, 'MDW-BLZ02-M-SYH', '{"beden":"M","renk":"Siyah"}'::jsonb, 199.00, 299.00, 33, 'ACTIVE'),
(2, 'MDW-BLZ02-L-BYZ', '{"beden":"L","renk":"Beyaz"}'::jsonb, 199.00, 299.00, 33, 'ACTIVE'),

-- Yüksek Bel Skinny Jean (3)
(3, 'MDW-JNS03-S-MAV', '{"beden":"S","renk":"Mavi"}'::jsonb,  599.00, NULL, NULL, 'ACTIVE'),
(3, 'MDW-JNS03-M-SYH', '{"beden":"M","renk":"Siyah"}'::jsonb, 599.00, NULL, NULL, 'ACTIVE'),
(3, 'MDW-JNS03-L-MAV', '{"beden":"L","renk":"Mavi"}'::jsonb,  599.00, NULL, NULL, 'ACTIVE'),

-- Baskılı Crop Top (4) — %17 indirim
(4, 'DFC-CRP04-XS-BYZ', '{"beden":"XS","renk":"Beyaz"}'::jsonb, 249.00, 299.00, 17, 'ACTIVE'),
(4, 'DFC-CRP04-S-SYH',  '{"beden":"S","renk":"Siyah"}'::jsonb,  249.00, 299.00, 17, 'ACTIVE'),
(4, 'DFC-CRP04-M-PMB',  '{"beden":"M","renk":"Pembe"}'::jsonb,  249.00, 299.00, 17, 'ACTIVE'),
(4, 'DFC-CRP04-L-BYZ',  '{"beden":"L","renk":"Beyaz"}'::jsonb,  249.00, 299.00, 17, 'ACTIVE'),

-- Saten Gece Elbisesi (5)
(5, 'TWS-ELB05-S-SYH', '{"beden":"S","renk":"Siyah"}'::jsonb,    1299.00, NULL, NULL, 'ACTIVE'),
(5, 'TWS-ELB05-M-KRM', '{"beden":"M","renk":"Kırmızı"}'::jsonb,  1299.00, NULL, NULL, 'ACTIVE'),
(5, 'TWS-ELB05-L-LCN', '{"beden":"L","renk":"Lacivert"}'::jsonb, 1299.00, NULL, NULL, 'ACTIVE'),

-- Triko Kazak (6) — %20 indirim
(6, 'MGO-KZK06-XS-BEJ', '{"beden":"XS","renk":"Bej"}'::jsonb,  899.00, 1129.00, 20, 'ACTIVE'),
(6, 'MGO-KZK06-S-SYH',  '{"beden":"S","renk":"Siyah"}'::jsonb,  899.00, 1129.00, 20, 'ACTIVE'),
(6, 'MGO-KZK06-M-KRM',  '{"beden":"M","renk":"Krem"}'::jsonb,   899.00, 1129.00, 20, 'ACTIVE'),
(6, 'MGO-KZK06-L-BEJ',  '{"beden":"L","renk":"Bej"}'::jsonb,    899.00, 1129.00, 20, 'ACTIVE'),

-- Pileli Mini Etek (7)
(7, 'ZRA-ETK07-XS-SYH', '{"beden":"XS","renk":"Siyah"}'::jsonb,  699.00, NULL, NULL, 'ACTIVE'),
(7, 'ZRA-ETK07-S-BYZ',  '{"beden":"S","renk":"Beyaz"}'::jsonb,   699.00, NULL, NULL, 'ACTIVE'),
(7, 'ZRA-ETK07-M-KRM',  '{"beden":"M","renk":"Kırmızı"}'::jsonb, 699.00, NULL, NULL, 'ACTIVE'),
(7, 'ZRA-ETK07-L-SYH',  '{"beden":"L","renk":"Siyah"}'::jsonb,   699.00, NULL, NULL, 'ACTIVE'),

-- Keten Gömlek (8) — %25 indirim
(8, 'HM-GMK08-XS-BYZ', '{"beden":"XS","renk":"Beyaz"}'::jsonb,    499.00, 669.00, 25, 'ACTIVE'),
(8, 'HM-GMK08-S-BEJ',  '{"beden":"S","renk":"Bej"}'::jsonb,       499.00, 669.00, 25, 'ACTIVE'),
(8, 'HM-GMK08-M-MAV',  '{"beden":"M","renk":"Açık Mavi"}'::jsonb, 499.00, 669.00, 25, 'ACTIVE'),
(8, 'HM-GMK08-L-BYZ',  '{"beden":"L","renk":"Beyaz"}'::jsonb,     499.00, 669.00, 25, 'ACTIVE'),
(8, 'HM-GMK08-XL-BEJ', '{"beden":"XL","renk":"Bej"}'::jsonb,      499.00, 669.00, 25, 'ACTIVE'),

-- ══ ERKEK GİYİM ══════════════════════════════════════════════════════════════

-- Slim Fit Oxford Gömlek (9) — %27 indirim
(9, 'ERK-GMK09-S-BYZ',  '{"beden":"S","renk":"Beyaz"}'::jsonb, 399.00, 549.00, 27, 'ACTIVE'),
(9, 'ERK-GMK09-M-BYZ',  '{"beden":"M","renk":"Beyaz"}'::jsonb, 399.00, 549.00, 27, 'ACTIVE'),
(9, 'ERK-GMK09-L-MAV',  '{"beden":"L","renk":"Mavi"}'::jsonb,  399.00, 549.00, 27, 'ACTIVE'),
(9, 'ERK-GMK09-XL-GRI', '{"beden":"XL","renk":"Gri"}'::jsonb,  399.00, 549.00, 27, 'ACTIVE'),

-- Regular Fit Chino Pantolon (10)
(10, 'KTN-CHN10-28-BEJ', '{"beden":"28","renk":"Bej"}'::jsonb,      649.00, NULL, NULL, 'ACTIVE'),
(10, 'KTN-CHN10-30-HKI', '{"beden":"30","renk":"Haki"}'::jsonb,     649.00, NULL, NULL, 'ACTIVE'),
(10, 'KTN-CHN10-32-LCN', '{"beden":"32","renk":"Lacivert"}'::jsonb, 649.00, NULL, NULL, 'ACTIVE'),
(10, 'KTN-CHN10-34-BEJ', '{"beden":"34","renk":"Bej"}'::jsonb,      649.00, NULL, NULL, 'ACTIVE'),

-- Oversize Baskılı Tişört (11) — %28 indirim
(11, 'ERK-TSH11-S-GRI',  '{"beden":"S","renk":"Gri"}'::jsonb,   249.00, 349.00, 28, 'ACTIVE'),
(11, 'ERK-TSH11-M-SYH',  '{"beden":"M","renk":"Siyah"}'::jsonb, 249.00, 349.00, 28, 'ACTIVE'),
(11, 'ERK-TSH11-L-BYZ',  '{"beden":"L","renk":"Beyaz"}'::jsonb, 249.00, 349.00, 28, 'ACTIVE'),
(11, 'ERK-TSH11-XL-GRI', '{"beden":"XL","renk":"Gri"}'::jsonb,  249.00, 349.00, 28, 'ACTIVE'),

-- Slim Fit Blazer (12)
(12, 'ZRM-BLZ12-S-LCN',  '{"beden":"S","renk":"Lacivert"}'::jsonb,  1899.00, NULL, NULL, 'ACTIVE'),
(12, 'ZRM-BLZ12-M-GRI',  '{"beden":"M","renk":"Gri"}'::jsonb,       1899.00, NULL, NULL, 'ACTIVE'),
(12, 'ZRM-BLZ12-L-BEJ',  '{"beden":"L","renk":"Bej"}'::jsonb,       1899.00, NULL, NULL, 'ACTIVE'),
(12, 'ZRM-BLZ12-XL-LCN', '{"beden":"XL","renk":"Lacivert"}'::jsonb, 1899.00, NULL, NULL, 'ACTIVE'),

-- Jogger Eşofman Altı (13)
(13, 'NKE-JGR13-S-SYH',  '{"beden":"S","renk":"Siyah"}'::jsonb,    1099.00, NULL, NULL, 'ACTIVE'),
(13, 'NKE-JGR13-M-GRI',  '{"beden":"M","renk":"Gri"}'::jsonb,      1099.00, NULL, NULL, 'ACTIVE'),
(13, 'NKE-JGR13-L-LCN',  '{"beden":"L","renk":"Lacivert"}'::jsonb, 1099.00, NULL, NULL, 'ACTIVE'),
(13, 'NKE-JGR13-XL-SYH', '{"beden":"XL","renk":"Siyah"}'::jsonb,   1099.00, NULL, NULL, 'ACTIVE'),

-- Kapüşonlu Sweatshirt (14) — %21 indirim
(14, 'DFC-SWT14-S-GRI',  '{"beden":"S","renk":"Gri"}'::jsonb,     449.00, 569.00, 21, 'ACTIVE'),
(14, 'DFC-SWT14-M-SYH',  '{"beden":"M","renk":"Siyah"}'::jsonb,   449.00, 569.00, 21, 'ACTIVE'),
(14, 'DFC-SWT14-L-LCN',  '{"beden":"L","renk":"Lacivert"}'::jsonb, 449.00, 569.00, 21, 'ACTIVE'),
(14, 'DFC-SWT14-XL-GRI', '{"beden":"XL","renk":"Gri"}'::jsonb,    449.00, 569.00, 21, 'ACTIVE'),

-- Slim Fit Kot Pantolon (15)
(15, 'MAV-KOT15-28-IND',  '{"beden":"28","renk":"Koyu İndigo"}'::jsonb, 999.00, NULL, NULL, 'ACTIVE'),
(15, 'MAV-KOT15-30-SYH',  '{"beden":"30","renk":"Siyah"}'::jsonb,       999.00, NULL, NULL, 'ACTIVE'),
(15, 'MAV-KOT15-32-AMAV', '{"beden":"32","renk":"Açık Mavi"}'::jsonb,   999.00, NULL, NULL, 'ACTIVE'),
(15, 'MAV-KOT15-34-IND',  '{"beden":"34","renk":"Koyu İndigo"}'::jsonb, 999.00, NULL, NULL, 'ACTIVE'),

-- Rüzgarlık Mont (16) — %20 indirim
(16, 'CLB-MNT16-S-SYH',  '{"beden":"S","renk":"Siyah"}'::jsonb,   2999.00, 3749.00, 20, 'ACTIVE'),
(16, 'CLB-MNT16-M-MAV',  '{"beden":"M","renk":"Mavi"}'::jsonb,    2999.00, 3749.00, 20, 'ACTIVE'),
(16, 'CLB-MNT16-L-YSL',  '{"beden":"L","renk":"Yeşil"}'::jsonb,   2999.00, 3749.00, 20, 'ACTIVE'),
(16, 'CLB-MNT16-XL-SYH', '{"beden":"XL","renk":"Siyah"}'::jsonb,  2999.00, 3749.00, 20, 'ACTIVE'),

-- ══ ÇOCUK GİYİM ══════════════════════════════════════════════════════════════

-- Çocuk Baskılı Pijama Takımı (17)
(17, 'LCW-PJM17-34YS',   '{"beden":"3-4 Yaş"}'::jsonb,  249.00, NULL, NULL, 'ACTIVE'),
(17, 'LCW-PJM17-56YS',   '{"beden":"5-6 Yaş"}'::jsonb,  249.00, NULL, NULL, 'ACTIVE'),
(17, 'LCW-PJM17-78YS',   '{"beden":"7-8 Yaş"}'::jsonb,  279.00, NULL, NULL, 'ACTIVE'),
(17, 'LCW-PJM17-910YS',  '{"beden":"9-10 Yaş"}'::jsonb, 279.00, NULL, NULL, 'ACTIVE'),

-- Kız Çocuğu Fiyonklu Elbise (18)
(18, 'ZRK-ELB18-4YS-PMB',  '{"beden":"4 Yaş","renk":"Pembe"}'::jsonb,  699.00, NULL, NULL, 'ACTIVE'),
(18, 'ZRK-ELB18-6YS-BYZ',  '{"beden":"6 Yaş","renk":"Beyaz"}'::jsonb,  699.00, NULL, NULL, 'ACTIVE'),
(18, 'ZRK-ELB18-8YS-LLA',  '{"beden":"8 Yaş","renk":"Lila"}'::jsonb,   699.00, NULL, NULL, 'ACTIVE'),
(18, 'ZRK-ELB18-10YS-PMB', '{"beden":"10 Yaş","renk":"Pembe"}'::jsonb, 699.00, NULL, NULL, 'ACTIVE'),

-- Erkek Çocuk Eşofman Takımı (19)
(19, 'HMK-ESF19-45YS-LCN',   '{"beden":"4-5 Yaş","renk":"Lacivert"}'::jsonb,  399.00, NULL, NULL, 'ACTIVE'),
(19, 'HMK-ESF19-67YS-GRI',   '{"beden":"6-7 Yaş","renk":"Gri"}'::jsonb,       399.00, NULL, NULL, 'ACTIVE'),
(19, 'HMK-ESF19-89YS-LCN',   '{"beden":"8-9 Yaş","renk":"Lacivert"}'::jsonb,  429.00, NULL, NULL, 'ACTIVE'),
(19, 'HMK-ESF19-1011YS-GRI', '{"beden":"10-11 Yaş","renk":"Gri"}'::jsonb,     429.00, NULL, NULL, 'ACTIVE'),

-- Bebek Tulum (20)
(20, 'MTC-TLM20-03AY-BYZ',  '{"beden":"0-3 Ay","renk":"Beyaz"}'::jsonb,  299.00, NULL, NULL, 'ACTIVE'),
(20, 'MTC-TLM20-36AY-SAR',  '{"beden":"3-6 Ay","renk":"Sarı"}'::jsonb,   299.00, NULL, NULL, 'ACTIVE'),
(20, 'MTC-TLM20-69AY-MNT',  '{"beden":"6-9 Ay","renk":"Mint"}'::jsonb,   329.00, NULL, NULL, 'ACTIVE'),
(20, 'MTC-TLM20-912AY-BYZ', '{"beden":"9-12 Ay","renk":"Beyaz"}'::jsonb, 329.00, NULL, NULL, 'ACTIVE'),

-- ══ TELEFON & AKSESUARLAR ════════════════════════════════════════════════════

-- Samsung Galaxy S24 (21) — indirimli
(21, 'SAM-S24-128-SYH', '{"depolama":"128 GB","renk":"Siyah","ram":"8 GB"}'::jsonb,  19999.00, 23999.00, 16, 'ACTIVE'),
(21, 'SAM-S24-256-SYH', '{"depolama":"256 GB","renk":"Siyah","ram":"8 GB"}'::jsonb,  22999.00, 26999.00, 14, 'ACTIVE'),
(21, 'SAM-S24-512-GRI', '{"depolama":"512 GB","renk":"Gri","ram":"12 GB"}'::jsonb,   29999.00, 34999.00, 14, 'ACTIVE'),

-- Apple iPhone 15 (22)
(22, 'APL-IP15-128-SYH', '{"depolama":"128 GB","renk":"Siyah"}'::jsonb,  18999.00, NULL, NULL, 'ACTIVE'),
(22, 'APL-IP15-128-MAV', '{"depolama":"128 GB","renk":"Mavi"}'::jsonb,   18999.00, NULL, NULL, 'ACTIVE'),
(22, 'APL-IP15-256-SYH', '{"depolama":"256 GB","renk":"Siyah"}'::jsonb,  21999.00, NULL, NULL, 'ACTIVE'),
(22, 'APL-IP15-256-BYZ', '{"depolama":"256 GB","renk":"Beyaz"}'::jsonb,  21999.00, NULL, NULL, 'ACTIVE'),

-- Xiaomi Redmi Note 13 Pro (23) — %10 indirim
(23, 'XMI-RN13P-128-SYH', '{"depolama":"128 GB","ram":"8 GB","renk":"Siyah"}'::jsonb,  12999.00, 14499.00, 10, 'ACTIVE'),
(23, 'XMI-RN13P-256-BYZ', '{"depolama":"256 GB","ram":"12 GB","renk":"Beyaz"}'::jsonb, 15999.00, 17799.00, 10, 'ACTIVE'),
(23, 'XMI-RN13P-256-YSL', '{"depolama":"256 GB","ram":"12 GB","renk":"Yeşil"}'::jsonb, 15999.00, 17799.00, 10, 'ACTIVE'),

-- Apple iPhone 15 Pro Max (24)
(24, 'APL-IP15PM-256-SYH', '{"depolama":"256 GB","renk":"Siyah Titanyum"}'::jsonb,   34999.00, NULL, NULL, 'ACTIVE'),
(24, 'APL-IP15PM-256-BYZ', '{"depolama":"256 GB","renk":"Beyaz Titanyum"}'::jsonb,   34999.00, NULL, NULL, 'ACTIVE'),
(24, 'APL-IP15PM-512-NT',  '{"depolama":"512 GB","renk":"Doğal Titanyum"}'::jsonb,   43999.00, NULL, NULL, 'ACTIVE'),

-- Apple AirPods Pro 2. Nesil (25)
(25, 'APL-APP2-USBC',    '{"model":"USB-C"}'::jsonb,    7999.00, NULL, NULL, 'ACTIVE'),
(25, 'APL-APP2-MAGSAFE', '{"model":"MagSafe"}'::jsonb,  7499.00, NULL, NULL, 'ACTIVE'),

-- Samsung Galaxy Tab S9 (26)
(26, 'SAM-TABS9-128-GRF', '{"depolama":"128 GB","ram":"8 GB","renk":"Grafit"}'::jsonb,  22999.00, NULL, NULL, 'ACTIVE'),
(26, 'SAM-TABS9-128-LVN', '{"depolama":"128 GB","ram":"8 GB","renk":"Lavanta"}'::jsonb, 22999.00, NULL, NULL, 'ACTIVE'),
(26, 'SAM-TABS9-256-BEJ', '{"depolama":"256 GB","ram":"12 GB","renk":"Bej"}'::jsonb,    27999.00, NULL, NULL, 'ACTIVE'),

-- Google Pixel 8 (27) — %10 indirim
(27, 'GGL-PX8-128-OBS', '{"depolama":"128 GB","renk":"Obsidyen"}'::jsonb, 21999.00, 24499.00, 10, 'ACTIVE'),
(27, 'GGL-PX8-128-KDF', '{"depolama":"128 GB","renk":"Kadifeye"}'::jsonb, 21999.00, 24499.00, 10, 'ACTIVE'),
(27, 'GGL-PX8-256-PMB', '{"depolama":"256 GB","renk":"Pembe"}'::jsonb,    25999.00, 28999.00, 10, 'ACTIVE'),

-- Huawei Watch GT 4 (28)
(28, 'HWI-WGT4-41-GMS', '{"kasa":"41 mm","renk":"Gümüş"}'::jsonb, 4999.00, NULL, NULL, 'ACTIVE'),
(28, 'HWI-WGT4-46-SYH', '{"kasa":"46 mm","renk":"Siyah"}'::jsonb, 6499.00, NULL, NULL, 'ACTIVE'),
(28, 'HWI-WGT4-41-ALT', '{"kasa":"41 mm","renk":"Altın"}'::jsonb,  5499.00, NULL, NULL, 'ACTIVE'),

-- ══ AYAKKABI ═════════════════════════════════════════════════════════════════

-- Nike Air Max 270 (29) — %17 indirim
(29, 'NKE-AM270-40-SYH', '{"numara":"40","renk":"Siyah"}'::jsonb,   3299.00, 3999.00, 17, 'ACTIVE'),
(29, 'NKE-AM270-41-KRM', '{"numara":"41","renk":"Kırmızı"}'::jsonb, 3299.00, 3999.00, 17, 'ACTIVE'),
(29, 'NKE-AM270-42-KRM', '{"numara":"42","renk":"Kırmızı"}'::jsonb, 3299.00, 3999.00, 17, 'ACTIVE'),
(29, 'NKE-AM270-43-BYZ', '{"numara":"43","renk":"Beyaz"}'::jsonb,   3299.00, 3999.00, 17, 'ACTIVE'),
(29, 'NKE-AM270-44-SYH', '{"numara":"44","renk":"Siyah"}'::jsonb,   3299.00, 3999.00, 17, 'ACTIVE'),

-- Adidas Classic Leather Sneaker (30) — %15 indirim
(30, 'ADS-CLS30-39-BYZ', '{"numara":"39","renk":"Beyaz"}'::jsonb, 2999.00, 3549.00, 15, 'ACTIVE'),
(30, 'ADS-CLS30-40-SYH', '{"numara":"40","renk":"Siyah"}'::jsonb, 2999.00, 3549.00, 15, 'ACTIVE'),
(30, 'ADS-CLS30-41-BYZ', '{"numara":"41","renk":"Beyaz"}'::jsonb, 2999.00, 3549.00, 15, 'ACTIVE'),
(30, 'ADS-CLS30-42-SYH', '{"numara":"42","renk":"Siyah"}'::jsonb, 2999.00, 3549.00, 15, 'ACTIVE'),
(30, 'ADS-CLS30-43-BYZ', '{"numara":"43","renk":"Beyaz"}'::jsonb, 2999.00, 3549.00, 15, 'ACTIVE'),
(30, 'ADS-CLS30-44-SYH', '{"numara":"44","renk":"Siyah"}'::jsonb, 2999.00, 3549.00, 15, 'ACTIVE'),

-- Yüksek Topuklu Stiletto (31)
(31, 'ALD-STL31-36-SYH', '{"numara":"36","renk":"Siyah"}'::jsonb,    1799.00, NULL, NULL, 'ACTIVE'),
(31, 'ALD-STL31-37-NUD', '{"numara":"37","renk":"Nude"}'::jsonb,     1799.00, NULL, NULL, 'ACTIVE'),
(31, 'ALD-STL31-38-KRM', '{"numara":"38","renk":"Kırmızı"}'::jsonb,  1799.00, NULL, NULL, 'ACTIVE'),
(31, 'ALD-STL31-39-SYH', '{"numara":"39","renk":"Siyah"}'::jsonb,    1799.00, NULL, NULL, 'ACTIVE'),
(31, 'ALD-STL31-40-NUD', '{"numara":"40","renk":"Nude"}'::jsonb,     1799.00, NULL, NULL, 'ACTIVE'),

-- Columbia Yürüyüş Botu (32) — %15 indirim
(32, 'CLB-BOT32-39-KHV', '{"numara":"39","renk":"Kahverengi"}'::jsonb, 5499.00, 6499.00, 15, 'ACTIVE'),
(32, 'CLB-BOT32-40-SYH', '{"numara":"40","renk":"Siyah"}'::jsonb,     5499.00, 6499.00, 15, 'ACTIVE'),
(32, 'CLB-BOT32-41-YSL', '{"numara":"41","renk":"Yeşil"}'::jsonb,     5499.00, 6499.00, 15, 'ACTIVE'),
(32, 'CLB-BOT32-42-KHV', '{"numara":"42","renk":"Kahverengi"}'::jsonb, 5499.00, 6499.00, 15, 'ACTIVE'),
(32, 'CLB-BOT32-43-SYH', '{"numara":"43","renk":"Siyah"}'::jsonb,     5499.00, 6499.00, 15, 'ACTIVE'),
(32, 'CLB-BOT32-44-YSL', '{"numara":"44","renk":"Yeşil"}'::jsonb,     5499.00, 6499.00, 15, 'ACTIVE'),

-- Zara Platform Loafer (33)
(33, 'ZRA-LFR33-36-SYH', '{"numara":"36","renk":"Siyah"}'::jsonb,      2499.00, NULL, NULL, 'ACTIVE'),
(33, 'ZRA-LFR33-37-BEJ', '{"numara":"37","renk":"Bej"}'::jsonb,        2499.00, NULL, NULL, 'ACTIVE'),
(33, 'ZRA-LFR33-38-KHV', '{"numara":"38","renk":"Kahverengi"}'::jsonb,  2499.00, NULL, NULL, 'ACTIVE'),
(33, 'ZRA-LFR33-39-SYH', '{"numara":"39","renk":"Siyah"}'::jsonb,      2499.00, NULL, NULL, 'ACTIVE'),
(33, 'ZRA-LFR33-40-BEJ', '{"numara":"40","renk":"Bej"}'::jsonb,        2499.00, NULL, NULL, 'ACTIVE'),

-- Nike Basketbol Ayakkabısı (34)
(34, 'NKE-BSK34-40-SYKRM', '{"numara":"40","renk":"Siyah/Kırmızı"}'::jsonb, 4499.00, NULL, NULL, 'ACTIVE'),
(34, 'NKE-BSK34-41-BZMAV', '{"numara":"41","renk":"Beyaz/Mavi"}'::jsonb,    4499.00, NULL, NULL, 'ACTIVE'),
(34, 'NKE-BSK34-42-SYKRM', '{"numara":"42","renk":"Siyah/Kırmızı"}'::jsonb, 4499.00, NULL, NULL, 'ACTIVE'),
(34, 'NKE-BSK34-43-BZMAV', '{"numara":"43","renk":"Beyaz/Mavi"}'::jsonb,    4499.00, NULL, NULL, 'ACTIVE'),
(34, 'NKE-BSK34-44-SYKRM', '{"numara":"44","renk":"Siyah/Kırmızı"}'::jsonb, 4499.00, NULL, NULL, 'ACTIVE'),
(34, 'NKE-BSK34-45-BZMAV', '{"numara":"45","renk":"Beyaz/Mavi"}'::jsonb,    4499.00, NULL, NULL, 'ACTIVE'),

-- Converse Chuck Taylor All Star (35) — %17 indirim
(35, 'CNV-CT-37-LCN', '{"numara":"37","renk":"Lacivert"}'::jsonb, 1899.00, 2299.00, 17, 'ACTIVE'),
(35, 'CNV-CT-38-SYH', '{"numara":"38","renk":"Siyah"}'::jsonb,    1899.00, 2299.00, 17, 'ACTIVE'),
(35, 'CNV-CT-39-BYZ', '{"numara":"39","renk":"Beyaz"}'::jsonb,    1899.00, 2299.00, 17, 'ACTIVE'),
(35, 'CNV-CT-40-LCN', '{"numara":"40","renk":"Lacivert"}'::jsonb, 1899.00, 2299.00, 17, 'ACTIVE'),
(35, 'CNV-CT-41-KRM', '{"numara":"41","renk":"Kırmızı"}'::jsonb,  1899.00, 2299.00, 17, 'ACTIVE'),
(35, 'CNV-CT-42-SYH', '{"numara":"42","renk":"Siyah"}'::jsonb,    1899.00, 2299.00, 17, 'ACTIVE'),

-- New Balance 574 (36) — %10 indirim
(36, 'NB-574-37-LCN', '{"numara":"37","renk":"Lacivert"}'::jsonb, 3299.00, 3699.00, 10, 'ACTIVE'),
(36, 'NB-574-38-GRI', '{"numara":"38","renk":"Gri"}'::jsonb,      3299.00, 3699.00, 10, 'ACTIVE'),
(36, 'NB-574-39-YSL', '{"numara":"39","renk":"Yeşil"}'::jsonb,    3299.00, 3699.00, 10, 'ACTIVE'),
(36, 'NB-574-40-LCN', '{"numara":"40","renk":"Lacivert"}'::jsonb, 3299.00, 3699.00, 10, 'ACTIVE'),
(36, 'NB-574-41-GRI', '{"numara":"41","renk":"Gri"}'::jsonb,      3299.00, 3699.00, 10, 'ACTIVE'),
(36, 'NB-574-42-YSL', '{"numara":"42","renk":"Yeşil"}'::jsonb,    3299.00, 3699.00, 10, 'ACTIVE'),
(36, 'NB-574-43-LCN', '{"numara":"43","renk":"Lacivert"}'::jsonb, 3299.00, 3699.00, 10, 'ACTIVE'),
(36, 'NB-574-44-GRI', '{"numara":"44","renk":"Gri"}'::jsonb,      3299.00, 3699.00, 10, 'ACTIVE'),

-- ══ SPOR & OUTDOOR ════════════════════════════════════════════════════════════

-- Decathlon Yoga Matı (37)
(37, 'DCT-YOG37-MOR', '{"renk":"Mor"}'::jsonb,   399.00, NULL, NULL, 'ACTIVE'),
(37, 'DCT-YOG37-YSL', '{"renk":"Yeşil"}'::jsonb, 399.00, NULL, NULL, 'ACTIVE'),
(37, 'DCT-YOG37-SYH', '{"renk":"Siyah"}'::jsonb, 399.00, NULL, NULL, 'ACTIVE'),
(37, 'DCT-YOG37-MAV', '{"renk":"Mavi"}'::jsonb,  399.00, NULL, NULL, 'ACTIVE'),

-- Nutrend Protein Shaker (38)
(38, 'NTR-SHK38-SYH', '{"renk":"Siyah"}'::jsonb,    199.00, NULL, NULL, 'ACTIVE'),
(38, 'NTR-SHK38-KRM', '{"renk":"Kırmızı"}'::jsonb,  199.00, NULL, NULL, 'ACTIVE'),
(38, 'NTR-SHK38-MAV', '{"renk":"Mavi"}'::jsonb,     199.00, NULL, NULL, 'ACTIVE'),
(38, 'NTR-SHK38-YSL', '{"renk":"Yeşil"}'::jsonb,    199.00, NULL, NULL, 'ACTIVE'),

-- Outdoor Research Kayak Montu (39) — %20 indirim
(39, 'OTR-KYK39-S-KRM',  '{"beden":"S","renk":"Kırmızı"}'::jsonb,   6999.00, 8749.00, 20, 'ACTIVE'),
(39, 'OTR-KYK39-M-LCN',  '{"beden":"M","renk":"Lacivert"}'::jsonb,  6999.00, 8749.00, 20, 'ACTIVE'),
(39, 'OTR-KYK39-L-SYH',  '{"beden":"L","renk":"Siyah"}'::jsonb,    6999.00, 8749.00, 20, 'ACTIVE'),
(39, 'OTR-KYK39-XL-KRM', '{"beden":"XL","renk":"Kırmızı"}'::jsonb, 6999.00, 8749.00, 20, 'ACTIVE'),

-- Garmin Forerunner 265 GPS (40)
(40, 'GRM-FR265-SYKRM', '{"renk":"Siyah/Kırmızı"}'::jsonb, 13499.00, NULL, NULL, 'ACTIVE'),
(40, 'GRM-FR265-BZMAV', '{"renk":"Beyaz/Mavi"}'::jsonb,    13499.00, NULL, NULL, 'ACTIVE'),
(40, 'GRM-FR265-PMB',   '{"renk":"Pembe"}'::jsonb,         13499.00, NULL, NULL, 'ACTIVE'),

-- ══ BİLGİSAYAR & TABLET ══════════════════════════════════════════════════════

-- MacBook Air M2 (41) — %10 indirim
(41, 'APL-MBA-M2-8-256-GY',  '{"ram":"8 GB","depolama":"256 GB","renk":"Gece Yarısı"}'::jsonb, 42999.00, 47999.00, 10, 'ACTIVE'),
(41, 'APL-MBA-M2-8-512-GY',  '{"ram":"8 GB","depolama":"512 GB","renk":"Gece Yarısı"}'::jsonb, 50999.00, 55999.00, 10, 'ACTIVE'),
(41, 'APL-MBA-M2-16-256-SL', '{"ram":"16 GB","depolama":"256 GB","renk":"Gümüş"}'::jsonb,      52999.00, 57999.00, 10, 'ACTIVE'),
(41, 'APL-MBA-M2-16-512-SL', '{"ram":"16 GB","depolama":"512 GB","renk":"Gümüş"}'::jsonb,      59999.00, 65999.00, 10, 'ACTIVE'),

-- MacBook Pro M3 Pro 14" (42) — %5 indirim
(42, 'APL-MBP-M3P-18-512-SYH', '{"ram":"18 GB","depolama":"512 GB","renk":"Uzay Siyahı"}'::jsonb, 79999.00,  84999.00, 5, 'ACTIVE'),
(42, 'APL-MBP-M3P-18-1TB-GMS', '{"ram":"18 GB","depolama":"1 TB","renk":"Gümüş"}'::jsonb,         94999.00,  99999.00, 5, 'ACTIVE'),
(42, 'APL-MBP-M3P-36-1TB-SYH', '{"ram":"36 GB","depolama":"1 TB","renk":"Uzay Siyahı"}'::jsonb,  109999.00, 115999.00, 5, 'ACTIVE'),

-- Lenovo ThinkPad X1 Carbon Gen 11 (43) — %13 indirim
(43, 'LNV-X1C-I7-16-512', '{"ram":"16 GB","depolama":"512 GB"}'::jsonb, 37999.00, 43999.00, 13, 'ACTIVE'),
(43, 'LNV-X1C-I7-32-1TB', '{"ram":"32 GB","depolama":"1 TB"}'::jsonb,   49999.00, 55999.00, 13, 'ACTIVE'),

-- Asus ROG Strix G16 Gaming (44) — %10 indirim
(44, 'ASUS-ROG-G16-32-1TB-4070', '{"ram":"32 GB","depolama":"1 TB","gpu":"RTX 4070"}'::jsonb, 64999.00, 72999.00, 10, 'ACTIVE'),
(44, 'ASUS-ROG-G16-32-2TB-4080', '{"ram":"32 GB","depolama":"2 TB","gpu":"RTX 4080"}'::jsonb, 84999.00, 92999.00, 10, 'ACTIVE'),

-- Dell UltraSharp 27" 4K Monitor (45)
(45, 'DLL-US27-4K-BLK', '{"renk":"Siyah"}'::jsonb, 19999.00, NULL, NULL, 'ACTIVE'),

-- Logitech MX Master 3S Mouse (46) — %10 indirim
(46, 'LGT-MX3S-GRF', '{"renk":"Grafit"}'::jsonb,    3499.00, 3899.00, 10, 'ACTIVE'),
(46, 'LGT-MX3S-GRI', '{"renk":"Soluk Gri"}'::jsonb, 3499.00, 3899.00, 10, 'ACTIVE'),
(46, 'LGT-MX3S-PMB', '{"renk":"Pembe"}'::jsonb,     3499.00, 3899.00, 10, 'ACTIVE');

SELECT setval('products_id_seq', 46);
SELECT setval('product_variants_id_seq', (SELECT MAX(id) FROM product_variants));

UPDATE product_variants
SET attributes = '{"package":"Standart"}'::jsonb
WHERE attributes = '{}'::jsonb;
