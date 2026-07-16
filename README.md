# E-Commerce Project


<details open>
<summary><strong>English</strong></summary>

This project is a microservice-based e-commerce system developed as the final assignment for the n11 Bootcamp. The goal was to design a realistic e-commerce flow with separate services for user management, products, cart, orders, payments, stock, coupons, reviews and notifications.

## Architecture Overview

The project contains 12 microservices, 7 PostgreSQL databases and 5 main infrastructure services.

Microservices:

1. `discovery-service`
2. `config-service`
3. `api-gateway`
4. `user-service`
5. `product-service`
6. `cart-service`
7. `order-service`
8. `payment-service`
9. `stock-service`
10. `coupon-service`
11. `notification-service`
12. `review-service`

Databases:

1. `userdb`
2. `productdb`
3. `orderdb`
4. `paymentdb`
5. `stockdb`
6. `coupondb`
7. `reviewdb`

Infrastructure services:

1. `PostgreSQL` - relational data storage
2. `Redis` - cart data, token blacklist, password reset token and rate limiting
3. `RabbitMQ` - event-driven communication between services
4. `MinIO` - object storage for product images
5. `Elasticsearch` - product search and indexing

## Tech Stack

Backend:

- Java 21
- Spring Boot 3.5.x
- Spring Cloud 2025.x
- Spring Security
- Spring Cloud Gateway
- Spring Cloud Config
- Eureka Discovery Server
- Spring Data JPA
- Spring Data Redis
- Spring AMQP / RabbitMQ
- OpenFeign / service client structure
- Flyway
- PostgreSQL
- MinIO
- Elasticsearch
- JavaMailSender
- Iyzico Sandbox
- Docker / Docker Compose

Frontend:

- React 19
- TypeScript
- Vite
- React Router
- Axios
- Lucide React
- Page-based CSS files

## Project Structure

```text
n11bootcamp-final-ecommerce-private/
├── backend/
│   ├── api-gateway/
│   ├── cart-service/
│   ├── config-service/
│   ├── coupon-service/
│   ├── discovery-service/
│   ├── notification-service/
│   ├── order-service/
│   ├── payment-service/
│   ├── product-service/
│   ├── review-service/
│   ├── stock-service/
│   ├── user-service/
│   └── pom.xml
│
├── frontend/
│   ├── src/
│   │   ├── context/
│   │   ├── lib/
│   │   ├── pages/
│   │   ├── services/
│   │   └── shared/
│   ├── package.json
│   └── vite.config.ts
│
├── infra/
│   ├── docker-compose.yml
│   └── postgres/
│
├── .env.example
├── Jenkinsfile
└── README.md
```

Most backend services follow a layered structure:

```text
service-name/
├── src/main/java/.../
│   ├── application/
│   ├── domain/
│   ├── infrastructure/
│   └── presentation/
└── src/main/resources/
    ├── application.yaml
    └── db/migration/
```

The layer responsibilities are:

- `domain`: entities, domain exceptions, repository contracts and core domain concepts
- `application`: business use cases and service logic
- `infrastructure`: integrations such as Redis, RabbitMQ, MinIO, Elasticsearch and external providers
- `presentation`: REST controllers, DTOs and API exception handling

## API Gateway

The API Gateway is the single entry point for the frontend. The frontend does not call individual services directly; every request goes through the gateway first.

Responsibilities:

- Route management
- JWT access token validation
- Rejecting refresh tokens for regular API calls
- Forwarding user context to downstream services
- Role checks for admin endpoints
- Public endpoint configuration
- CORS management
- Correlation ID generation
- Request logging

Because of this structure, each service does not need to repeat JWT parsing and detailed security configuration. The gateway validates the token, extracts the user context and forwards `X-User-Id`, `X-User-Email` and `X-User-Role` headers to downstream services.

## User Service

The user-service manages authentication and account data.

Responsibilities:

- Register, login, refresh token and logout
- Profile management
- Address management
- JWT access and refresh token generation
- Storing tokens in HttpOnly cookies
- Redis-based token blacklist
- Redis-based password reset token storage
- Redis-based auth endpoint rate limiting
- Publishing user registration and password reset events with RabbitMQ
- Storing users and addresses in PostgreSQL
- Hashing passwords with BCrypt

`InternalUserController` was prepared for future inter-service user lookups. It is currently not used by another service.

## Product Service

The product-service manages products, categories, variants and product images.

Responsibilities:

- Product CRUD
- Category management
- Variant management
- Product image upload
- MinIO object storage integration
- Elasticsearch product indexing and search
- Publishing product and variant events with RabbitMQ

Product images are not stored directly in the database. They are stored in MinIO, while searchable product data is indexed into Elasticsearch.

## Stock Service

The stock-service manages product and variant stock.

Responsibilities:

- Creating stock records
- Variant-level stock management
- Increasing and decreasing stock
- Recording stock movements
- Listening to product-service events through RabbitMQ
- Publishing stock events

Stock logic is separated from the product-service because stock movements are directly related to order processing and belong to a separate domain.

## Cart Service

The cart-service manages user carts.

Responsibilities:

- Adding products to cart
- Updating item quantity
- Removing items from cart
- Clearing the cart
- Storing cart data in Redis
- Coupon preview integration with coupon-service
- Clearing the cart after order confirmation events

Cart data changes frequently and needs fast access, so Redis is used instead of a relational database.

## Coupon Service

The coupon-service manages coupon creation, validation and usage.

Responsibilities:

- Creating coupons
- Updating coupons
- Activating and deactivating coupons
- Validating coupons
- Using coupons
- Tracking user-based usage
- Checking minimum order amount
- Checking coupon validity dates
- Reducing concurrent usage risks with optimistic locking
- Managing coupon seed data with Flyway

Coupon usage is not treated as a simple code check. The service tracks usage limits, usage records, validity dates and user-based constraints.

## Order Service

The order-service manages order creation and the order lifecycle.

Responsibilities:

- Creating orders
- Reading product data from product-service
- Checking and decreasing stock through stock-service
- Validating and using coupons through coupon-service
- Starting payment through payment-service
- Managing order statuses
- Publishing order confirmed, failed and cancelled events with RabbitMQ

The order-service acts as the orchestration layer of the system. It coordinates product, stock, coupon and payment services to produce the final order result.

The order flow uses an orchestration-based Saga approach. The order-service first creates the order as `PENDING`, reserves stock, starts payment and then finalizes the order as `CONFIRMED`. If stock reservation or payment fails, it runs compensation logic by releasing the stock that was already reserved and marks the order as `FAILED`.

## Payment Service

The payment-service manages payment processing.

Responsibilities:

- Processing payment requests
- Recording payment transactions
- Iyzico sandbox integration
- Fake payment provider support for local testing
- Abstracting payment providers with the provider pattern
- Publishing payment completed and failed events with RabbitMQ

Payment providers are kept behind an interface. This makes it possible to use a fake provider locally and an Iyzico provider in a sandbox environment.

## Notification Service

The notification-service manages email notifications.

Responsibilities:

- Listening to RabbitMQ events
- Welcome email
- Password reset email
- Order status emails
- SMTP email sending with JavaMailSender
- HTML template management
- Plain text fallback

The user-service and order-service do not send emails directly. They publish events through RabbitMQ, and the notification-service handles email delivery separately.

## Review Service

The review-service manages product reviews.

Responsibilities:

- Creating product reviews
- Listing reviews by product
- User-based review management
- Admin review deletion
- Storing review data in PostgreSQL

Review data is kept separate from the product-service. Product detail pages load review data from the review-service.

## Database and Migration Management

PostgreSQL is used as the relational database. Each main domain has its own database.

Flyway is used for database migration management. Table creation, column changes and seed data are tracked with versioned SQL migration files. This allows every service database schema to be created automatically and consistently when the project starts from scratch.

## Observability Note

Grafana, Loki, Prometheus and Promtail integration was also prepared for observability. However, these services were removed from the active deployment compose setup because the public deployment server had limited RAM.

They can be re-enabled by updating the Docker Compose configuration if metrics and log monitoring are needed.

## Frontend

The frontend is built with React, TypeScript and Vite. Pages are organized by domain.

Responsibilities:

- Auth state management
- Cart state management
- Guest cart support
- Guest cart sync after login
- Product listing and product detail pages
- Cart and coupon flow
- Checkout and payment flow
- Order success and order listing pages
- Admin panel
- API service layer separation

The frontend does not store tokens in localStorage. Authentication is handled through HttpOnly cookies. API requests are sent through the API Gateway.

## Running

Real `.env` files should not be added to the project.

Environment files should be placed like this:

```text
n11bootcamp-final-ecommerce-private/
├── .env.example
├── infra/
│   ├── .env
│   └── docker-compose.yml
└── frontend/
    ├── .env
    └── .env.example
```

Start backend infrastructure and services:

```bash
cd infra
docker compose up -d --build
```

Start frontend:

```bash
cd frontend
npm install
npm run dev
```

Default local addresses:

- Frontend: `http://localhost:5173`
- API Gateway: `http://localhost:8763`
- Eureka Dashboard: `http://localhost:8761`
- RabbitMQ Management: `http://localhost:15672`
- MinIO Console: `http://localhost:9001`

</details>

<details>
<summary><strong>Türkçe</strong></summary>

Bu proje, n11 Bootcamp final ödevi kapsamında geliştirilmiş mikroservis tabanlı bir e-ticaret sistemidir. Amaç; kullanıcı yönetimi, ürün yönetimi, sepet, sipariş, ödeme, stok, kupon, yorum ve bildirim süreçlerini gerçek bir e-ticaret akışına yakın şekilde ayrı servisler halinde tasarlamaktır.


## Genel Mimari

Projede 12 mikroservis, 7 PostgreSQL database ve 5 temel altyapı servisi bulunur.

Mikroservisler:

1. `discovery-service`
2. `config-service`
3. `api-gateway`
4. `user-service`
5. `product-service`
6. `cart-service`
7. `order-service`
8. `payment-service`
9. `stock-service`
10. `coupon-service`
11. `notification-service`
12. `review-service`

Databaseler:

1. `userdb`
2. `productdb`
3. `orderdb`
4. `paymentdb`
5. `stockdb`
6. `coupondb`
7. `reviewdb`

Temel altyapı servisleri:

1. `PostgreSQL` - ilişkisel veri saklama
2. `Redis` - sepet, token blacklist, password reset token ve rate limit
3. `RabbitMQ` - servisler arası event-driven iletişim
4. `MinIO` - ürün görselleri için object storage
5. `Elasticsearch` - ürün arama ve indeksleme

## Teknoloji Stack

Backend:

- Java 21
- Spring Boot 3.5.x
- Spring Cloud 2025.x
- Spring Security
- Spring Cloud Gateway
- Spring Cloud Config
- Eureka Discovery Server
- Spring Data JPA
- Spring Data Redis
- Spring AMQP / RabbitMQ
- OpenFeign / servis client yapısı
- Flyway
- PostgreSQL
- MinIO
- Elasticsearch
- JavaMailSender
- Iyzico Sandbox
- Docker / Docker Compose

Frontend:

- React 19
- TypeScript
- Vite
- React Router
- Axios
- Lucide React
- Sayfa bazlı CSS dosyaları

## Proje Yapısı

```text
n11bootcamp-final-ecommerce-private/
├── backend/
│   ├── api-gateway/
│   ├── cart-service/
│   ├── config-service/
│   ├── coupon-service/
│   ├── discovery-service/
│   ├── notification-service/
│   ├── order-service/
│   ├── payment-service/
│   ├── product-service/
│   ├── review-service/
│   ├── stock-service/
│   ├── user-service/
│   └── pom.xml
│
├── frontend/
│   ├── src/
│   │   ├── context/
│   │   ├── lib/
│   │   ├── pages/
│   │   ├── services/
│   │   └── shared/
│   ├── package.json
│   └── vite.config.ts
│
├── infra/
│   ├── docker-compose.yml
│   └── postgres/
│
├── .env.example
├── Jenkinsfile
└── README.md
```

Backend servisleri genel olarak katmanlı yapı ile organize edildi:

```text
service-name/
├── src/main/java/.../
│   ├── application/
│   ├── domain/
│   ├── infrastructure/
│   └── presentation/
└── src/main/resources/
    ├── application.yaml
    └── db/migration/
```

Katman sorumlulukları:

- `domain`: entity, domain exception, repository contract ve temel domain kavramları
- `application`: iş kuralları ve use-case mantığı
- `infrastructure`: Redis, RabbitMQ, MinIO, Elasticsearch ve external provider entegrasyonları
- `presentation`: REST controller, DTO ve API exception handling

## API Gateway

API Gateway, frontend tarafının tek giriş noktasıdır. Frontend doğrudan mikroservislere gitmez; bütün istekler önce gateway'e gelir.

Yaptıkları:

- Route yönetimi
- JWT access token doğrulama
- Refresh token ile normal API çağrılarını reddetme
- Kullanıcı bilgisini downstream servislere aktarma
- Admin endpointleri için role kontrolü
- Public endpointleri ayırma
- CORS yönetimi
- Correlation ID üretme
- Request logging

Bu yapı sayesinde her serviste tekrar JWT doğrulama veya detaylı security config yazmak gerekmez. Gateway token'ı doğrular, kullanıcı context bilgisini çıkarır ve downstream servislere `X-User-Id`, `X-User-Email`, `X-User-Role` headerlarıyla iletir.

## User Service

User-service kullanıcı kimlik doğrulama ve hesap yönetiminden sorumludur.

Yaptıkları:

- Register, login, refresh token ve logout
- Profil yönetimi
- Adres yönetimi
- JWT access token ve refresh token üretimi
- Tokenları HttpOnly cookie olarak client'a yazma
- Redis ile token blacklist
- Redis ile password reset token saklama
- Redis ile auth endpoint rate limit
- RabbitMQ ile kullanıcı kayıt ve password reset event publish
- PostgreSQL ile kullanıcı ve adres verilerini saklama
- BCrypt ile şifre hashleme


`InternalUserController`, ileride servisler arası kullanıcı bilgisi okumak için hazırlanmış internal endpointtir. Şu anda aktif olarak başka bir servis tarafından kullanılmaz.

## Product Service

Product-service ürün, kategori, varyant ve ürün görsel yönetiminden sorumludur.

Yaptıkları:

- Ürün CRUD
- Kategori yönetimi
- Varyant yönetimi
- Ürün görsel upload
- MinIO object storage entegrasyonu
- Elasticsearch ürün indeksleme ve arama
- RabbitMQ ile ürün/varyant event publish

Ürün görselleri veritabanına gömülmez; MinIO üzerinde saklanır. Arama yapılacak ürün verisi Elasticsearch'e indekslenir.

## Stock Service

Stock-service ürün ve varyant stoklarını yönetir.

Yaptıkları:

- Stok kaydı oluşturma
- Varyant bazlı stok yönetimi
- Stok ekleme ve azaltma
- Stok hareketlerini kaydetme
- Product-service eventlerini RabbitMQ üzerinden dinleme
- Stok eventlerini yayınlama

Stok yönetimi product-service içinde tutulmadı. Stok hareketleri sipariş süreciyle ilişkili olduğu için ayrı bir domain olarak ayrıldı.

## Cart Service

Cart-service kullanıcı sepetini yönetir.

Yaptıkları:

- Sepete ürün ekleme
- Sepet ürünü miktar güncelleme
- Sepetten ürün silme
- Sepeti temizleme
- Redis üzerinde sepet saklama
- Coupon-service ile kupon preview
- Order confirmed event sonrası sepet temizleme

Sepet verisi sık değişen ve hızlı erişilmesi gereken geçici veri olduğu için Redis kullanıldı.

## Coupon Service

Coupon-service kupon yönetiminden sorumludur.

Yaptıkları:

- Kupon oluşturma
- Kupon güncelleme
- Kupon aktif/pasif yönetimi
- Kupon doğrulama
- Kupon kullanma
- Kullanıcı bazlı kullanım takibi
- Minimum sipariş tutarı kontrolü
- Kupon geçerlilik tarihi kontrolü
- Optimistic locking ile eş zamanlı kullanım riskini azaltma
- Flyway ile kupon seed data yönetimi

Kupon kullanımı sadece kod kontrolü olarak tutulmadı; kullanıcı limiti, kullanım kaydı ve tarih kontrolleriyle yönetildi.

## Order Service

Order-service sipariş oluşturma ve sipariş yaşam döngüsünü yönetir.

Yaptıkları:

- Sipariş oluşturma
- Ürün bilgilerini product-service'ten alma
- Stok kontrolü ve stok düşme
- Kupon doğrulama ve kupon kullanma
- Payment-service ile ödeme başlatma
- Sipariş durumlarını yönetme
- RabbitMQ ile order confirmed, failed ve cancelled eventleri yayınlama

Order-service sistemin orchestration katmanı gibi çalışır. Product, stock, coupon ve payment servisleriyle haberleşerek sipariş sonucunu üretir.

Order akışı orchestration-based Saga yaklaşımıyla ilerler. Order-service önce siparişi `PENDING` olarak oluşturur, stoku reserve eder, ödemeyi başlatır ve başarılı olursa siparişi `CONFIRMED` durumuna çeker. Stok reserve veya ödeme adımında hata olursa compensation logic çalışır; daha önce reserve edilen stok geri bırakılır ve sipariş `FAILED` olarak işaretlenir. 

## Payment Service

Payment-service ödeme işlemlerinden sorumludur.

Yaptıkları:

- Ödeme isteğini işleme
- Payment transaction kaydı tutma
- Iyzico sandbox entegrasyonu
- Fake payment provider ile local test desteği
- Provider pattern ile ödeme sağlayıcısını soyutlama
- RabbitMQ ile payment completed ve failed eventleri yayınlama

Ödeme sağlayıcısı interface arkasına alındı. Bu sayede local ortamda fake provider, sandbox ortamda Iyzico provider kullanılabilir.

## Notification Service

Notification-service mail bildirimlerinden sorumludur.

Yaptıkları:

- RabbitMQ eventlerini dinleme
- Kullanıcı kayıt maili
- Şifre sıfırlama maili
- Sipariş onay/başarısız/iptal maili
- JavaMailSender ile SMTP mail gönderimi
- HTML template yönetimi
- Plain text fallback

User-service ve order-service doğrudan mail göndermez. RabbitMQ üzerinden event yayınlanır, notification-service bu eventleri dinleyip mail sürecini ayrı yönetir.

## Review Service

Review-service ürün yorumlarından sorumludur.

Yaptıkları:

- Ürün yorumu oluşturma
- Ürün yorumlarını listeleme
- Kullanıcı bazlı yorum yönetimi
- Admin yorum silme
- PostgreSQL ile review datasını saklama

Yorum domain'i product-service'ten ayrı tutuldu. Ürün detay sayfasında yorumlar ayrı servisten çekilir.

## Database ve Migration Yönetimi

Database tarafında PostgreSQL kullanıldı. Her ana domain kendi database'ine sahip olacak şekilde ayrıldı.

Flyway, database migration yönetimi için kullanıldı. Tablo oluşturma, kolon ekleme ve seed data gibi database değişiklikleri versiyonlu SQL dosyalarıyla takip edilir. Bu sayede proje sıfırdan ayağa kalktığında her servisin database şeması otomatik ve tutarlı şekilde oluşur.

## Observability Notu

Projeye observability tarafında Grafana, Loki, Prometheus ve Promtail entegrasyonu da eklenmiştir. Ancak public olarak deploy edilecek sunucunun RAM kapasitesi sınırlı olduğu için bu servisler aktif compose yapısından çıkarılmıştır.

İstenirse `docker-compose.yml` üzerinden tekrar eklenerek metrik ve log izleme altyapısı çalıştırılabilir.

## Frontend

Frontend tarafında React, TypeScript ve Vite kullanıldı. Sayfalar domain bazlı organize edildi.

Yaptıkları:

- Auth state yönetimi
- Cart state yönetimi
- Guest cart desteği
- Login sonrası guest cart sync
- Ürün listeleme ve ürün detay sayfaları
- Sepet ve kupon akışı
- Checkout ve payment akışı
- Sipariş başarı ve sipariş listeleme sayfaları
- Admin paneli
- API çağrılarını service layer'a ayırma

Frontend tokenları localStorage'da tutmaz. Authentication akışı HttpOnly cookie üzerinden ilerler. API istekleri gateway'e gönderilir.

## Çalıştırma

Projeye gerçek `.env` dosyaları eklenmemelidir.

Environment dosyaları şu şekilde yerleştirilmelidir:

```text
n11bootcamp-final-ecommerce-private/
├── .env.example
├── infra/
│   ├── .env
│   └── docker-compose.yml
└── frontend/
    ├── .env
    └── .env.example
```


Backend altyapısını ve servisleri Docker Compose ile ayağa kaldırmak için:

```bash
cd infra
docker compose up -d --build
```

Frontend için:

```bash
cd frontend
npm install
npm run dev
```

Varsayılan local adresler:

- Frontend: `http://localhost:5173`
- API Gateway: `http://localhost:8763`
- Eureka Dashboard: `http://localhost:8761`
- RabbitMQ Management: `http://localhost:15672`
- MinIO Console: `http://localhost:9001`


</details>
