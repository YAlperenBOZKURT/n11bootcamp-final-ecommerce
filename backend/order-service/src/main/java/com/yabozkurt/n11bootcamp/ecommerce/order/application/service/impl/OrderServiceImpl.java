package com.yabozkurt.n11bootcamp.ecommerce.order.application.service.impl;

import com.yabozkurt.n11bootcamp.ecommerce.order.application.service.OrderService;
import com.yabozkurt.n11bootcamp.ecommerce.order.domain.exception.OrderNotFoundException;
import com.yabozkurt.n11bootcamp.ecommerce.order.domain.exception.OrderValidationException;
import com.yabozkurt.n11bootcamp.ecommerce.order.domain.model.Order;
import com.yabozkurt.n11bootcamp.ecommerce.order.domain.model.OrderItem;
import com.yabozkurt.n11bootcamp.ecommerce.order.domain.model.enums.OrderStatus;
import com.yabozkurt.n11bootcamp.ecommerce.order.domain.repository.OrderRepository;
import com.yabozkurt.n11bootcamp.ecommerce.order.infrastructure.client.*;
import com.yabozkurt.n11bootcamp.ecommerce.order.infrastructure.client.dto.*;
import com.yabozkurt.n11bootcamp.ecommerce.order.infrastructure.messaging.event.OrderCancelledEvent;
import com.yabozkurt.n11bootcamp.ecommerce.order.infrastructure.messaging.event.OrderConfirmedEvent;
import com.yabozkurt.n11bootcamp.ecommerce.order.infrastructure.messaging.event.OrderFailedEvent;
import com.yabozkurt.n11bootcamp.ecommerce.order.infrastructure.messaging.publisher.OrderEventPublisher;
import com.yabozkurt.n11bootcamp.ecommerce.order.presentation.dto.request.CardRequest;
import com.yabozkurt.n11bootcamp.ecommerce.order.presentation.dto.request.OrderItemRequest;
import com.yabozkurt.n11bootcamp.ecommerce.order.presentation.dto.request.OrderRequest;
import com.yabozkurt.n11bootcamp.ecommerce.order.presentation.dto.response.OrderItemResponse;
import com.yabozkurt.n11bootcamp.ecommerce.order.presentation.dto.response.OrderResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class OrderServiceImpl implements OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderServiceImpl.class);

    private final OrderRepository orderRepository;
    private final ProductClient productClient;
    private final StockClient stockClient;
    private final PaymentClient paymentClient;
    private final CouponClient couponClient;
    private final OrderEventPublisher eventPublisher;
    private final SagaTransactionSupport sagaSupport;

    public OrderServiceImpl(OrderRepository orderRepository,
                            ProductClient productClient,
                            StockClient stockClient,
                            PaymentClient paymentClient,
                            CouponClient couponClient,
                            OrderEventPublisher eventPublisher,
                            SagaTransactionSupport sagaSupport) {
        this.orderRepository = orderRepository;
        this.productClient = productClient;
        this.stockClient = stockClient;
        this.paymentClient = paymentClient;
        this.couponClient = couponClient;
        this.eventPublisher = eventPublisher;
        this.sagaSupport = sagaSupport;
    }

    // -- placeOrder saga flow ------------------------------------------------
    // Intentionally not @Transactional because we call external services inside.
    // DB writes are done via SagaTransactionSupport with REQUIRES_NEW.

    @Override
    public OrderResponse placeOrder(Long userId, String userEmail, OrderRequest request) {

        // Step 1: Resolve item prices from product-service.
        List<ResolvedItem> resolvedItems;
        try {
            resolvedItems = resolveItems(request.getItems());
        } catch (OrderValidationException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("[SAGA] Product/price resolution failed: {}", ex.getMessage());
            throw new OrderValidationException("Ürün bilgileri alınamadı. Lütfen tekrar deneyin.");
        }

        BigDecimal totalAmount = computeTotal(resolvedItems);

        // Step 2: Check coupon validity (do not consume yet).
        CouponPreview coupon = preValidateCoupon(request.getCouponCode(), userId, totalAmount);

        // Step 3: Save order as PENDING.
        Order order = sagaSupport.savePending(
                buildOrder(userId, userEmail, resolvedItems, totalAmount,
                        coupon.discountAmount(), coupon.finalAmount()));
        String orderNumber = order.getOrderNumber();
        log.info("[SAGA] Order {} created — items={}, total={}, finalAmount={}",
                orderNumber, resolvedItems.size(), totalAmount, coupon.finalAmount());

        // Step 4: Reserve stock item by item.
        // If one item fails, release only already reserved ones.
        List<ResolvedItem> reservedItems = new ArrayList<>();
        
        try {
            for (ResolvedItem item : resolvedItems) {
                stockClient.reserveVariant(item.variantId(),
                        new StockReserveRequest(item.quantity(), "order:" + orderNumber));
                reservedItems.add(item);
                log.debug("[SAGA] Stock reserved — variantId={}, qty={}, order={}", item.variantId(), item.quantity(), orderNumber);
            }
        } catch (Exception ex) {
            log.error("[SAGA] Stock reservation failed for order={} after reserving {}/{} items: {}",
                    orderNumber, reservedItems.size(), resolvedItems.size(), ex.getMessage());
            releaseReservedStock(reservedItems, orderNumber);
            Order failed = sagaSupport.markFailed(order, friendlyStockError(ex));
            eventPublisher.publishOrderFailed(
                    new OrderFailedEvent(orderNumber, userId, userEmail, failed.getFailReason()));
            return toResponse(failed);
        }

        // Step 5: Process payment.
        // If payment fails, release reserved stock and mark order FAILED.
        PaymentResponse payment;
        try {
            payment = callPayment(orderNumber, userId, coupon.finalAmount(), request.getCard());
        } catch (Exception ex) {
            log.error("[SAGA] Payment call failed for order={}: {}", orderNumber, ex.getMessage());
            releaseReservedStock(reservedItems, orderNumber);
            Order failed = sagaSupport.markFailed(order,
                    "Ödeme servisi geçici olarak kullanılamıyor. Lütfen tekrar deneyin.");
            eventPublisher.publishOrderFailed(
                    new OrderFailedEvent(orderNumber, userId, userEmail, failed.getFailReason()));
            return toResponse(failed);
        }

        if (!payment.isSuccess()) {
            String declineReason = payment.getFailReason() != null
                    ? payment.getFailReason() : "Kart işlemi reddedildi.";
            log.warn("[SAGA] Payment declined for order={}: {}", orderNumber, declineReason);
            releaseReservedStock(reservedItems, orderNumber);
            Order failed = sagaSupport.markFailed(order, declineReason);
            eventPublisher.publishOrderFailed(
                    new OrderFailedEvent(orderNumber, userId, userEmail, declineReason));
            return toResponse(failed);
        }

        // Payment is captured from this point.
        // Remaining steps are best-effort and we do not roll back payment.
        log.info("[SAGA] Payment captured for order={} — starting post-payment finalization.", orderNumber);

        // Step 6: Confirm stock deduction (best-effort).
        confirmReservedStock(reservedItems, orderNumber);

        // Step 7: Consume coupon (best-effort).
        if (coupon.isApplied()) {
            consumeCoupon(request.getCouponCode(), userId, orderNumber, totalAmount);
        }

        // Step 8: Mark order CONFIRMED.
        // If this fails after payment capture, we log critical and ask for manual reconciliation.
        
        Order confirmed;
        try {
            confirmed = sagaSupport.markConfirmed(order,
                    coupon.discountAmount(), coupon.finalAmount(), request.getCouponCode());
        } catch (Exception ex) {
            log.error("[SAGA][CRITICAL] markConfirmed DB write failed for order={} AFTER payment captured. " +
                    "Manual reconciliation required! Error: {}", orderNumber, ex.getMessage());
            throw new IllegalStateException(
                    "Sipariş kaydedilirken bir hata oluştu. Ödeme alındı — lütfen destek ekibiyle iletişime geçin. " +
                    "Sipariş numaranız: " + orderNumber);
        }

        eventPublisher.publishOrderConfirmed(
                new OrderConfirmedEvent(orderNumber, userId, userEmail, coupon.finalAmount()));
        log.info("[SAGA] Order {} confirmed successfully.", orderNumber);
        return toResponse(confirmed);
    }

    // -- read operations -------------------------------------------------------

    @Override
    @Transactional(readOnly = true)
    public OrderResponse getByOrderNumber(Long userId, String role, String orderNumber) {
        Order order = orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new OrderNotFoundException("Sipariş bulunamadı: " + orderNumber));
        // Non-admin users cannot read other users' orders.
        if (!"ADMIN".equals(role) && !order.getUserId().equals(userId)) {
            throw new OrderNotFoundException("Sipariş bulunamadı: " + orderNumber);
        }
        return toResponse(order);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderResponse> getOrdersByUser(Long userId) {
        return orderRepository.findByUserIdWithItems(userId).stream()
                .map(OrderServiceImpl::toResponse)
                .collect(Collectors.toList());
    }

    // -- cancel ----------------------------------------------------------------
    // Only PENDING orders can be cancelled from here.

    @Override
    @Transactional
    public OrderResponse cancelOrder(Long userId, String orderNumber) {
        Order order = orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new OrderNotFoundException("Sipariş bulunamadı: " + orderNumber));

        if (!order.getUserId().equals(userId)) {
            throw new OrderNotFoundException("Sipariş bulunamadı: " + orderNumber);
        }

        switch (order.getStatus()) {
            case CONFIRMED -> throw new IllegalStateException(
                    "Onaylanmış siparişler bu ekrandan iptal edilemez. Lütfen destek ekibiyle iletişime geçin.");
            case FAILED, CANCELLED -> throw new IllegalStateException(
                    "Sipariş zaten " + order.getStatus().name().toLowerCase() + " durumunda.");
            case PENDING -> { /* proceed */ }
        }

        // Try to release reserved stock if any. Errors are logged and ignored.
        List<ResolvedItem> itemsToRelease = order.getItems().stream()
                .filter(i -> i.getVariantId() != null)
                .map(i -> new ResolvedItem(
                        i.getProductId(), i.getVariantId(),
                        i.getProductName(), i.getUnitPrice(), i.getQuantity()))
                .toList();
        releaseReservedStock(itemsToRelease, orderNumber);

        order.setStatus(OrderStatus.CANCELLED);
        order.setFailReason("Kullanıcı tarafından iptal edildi");
        order = orderRepository.save(order);

        eventPublisher.publishOrderCancelled(
                new OrderCancelledEvent(orderNumber, userId, order.getUserEmail(),
                        "Kullanıcı tarafından iptal edildi"));

        log.info("[SAGA] Order {} cancelled by user {}.", orderNumber, userId);
        return toResponse(order);
    }

    @Override
    public Page<OrderResponse> getAllOrders(Pageable pageable) {
        Page<Long> idPage = orderRepository.findAllOrderIds(pageable);

        if (idPage.isEmpty()) {
            return new PageImpl<>(List.of(), pageable, idPage.getTotalElements());
        }

        List<Order> loaded = orderRepository.findAllByIdInWithItems(idPage.getContent());
        Map<Long, Order> byId = new HashMap<>();
        for (Order o : loaded) {
            byId.put(o.getId(), o);
        }

        List<OrderResponse> content = idPage.getContent().stream()
                .map(byId::get)
                .filter(o -> o != null)
                .map(OrderServiceImpl::toResponse)
                .toList();

        return new PageImpl<>(content, pageable, idPage.getTotalElements());
    }

    // -- saga helpers ----------------------------------------------------------

    private List<ResolvedItem> resolveItems(List<OrderItemRequest> requests) {
        List<ResolvedItem> resolved = new ArrayList<>();
        for (OrderItemRequest req : requests) {
            if (req.getVariantId() == null) {
                throw new OrderValidationException(
                        "variantId zorunludur (productId=" + req.getProductId() + ").");
            }

            ApiResponse<ProductClientResponse> resp = productClient.getById(req.getProductId());
            if (!resp.isSuccess() || resp.getData() == null) {
                throw new OrderValidationException("Ürün bulunamadı: productId=" + req.getProductId());
            }

            ProductClientResponse product = resp.getData();
            List<VariantClientResponse> variants = product.getVariants();
            if (variants == null || variants.isEmpty()) {
                throw new OrderValidationException(
                        "Ürünün satışa açık varyantı yok: productId=" + req.getProductId());
            }

            Long variantId = req.getVariantId();
            BigDecimal unitPrice = variants.stream()
                    .filter(v -> v.getId().equals(variantId))
                    .map(VariantClientResponse::getEffectivePrice)
                    .findFirst()
                    .orElseThrow(() -> new OrderValidationException(
                            "Varyant bulunamadı: variantId=" + variantId));

            resolved.add(new ResolvedItem(req.getProductId(), variantId,
                    product.getName(), unitPrice, req.getQuantity()));
        }
        return resolved;
    }

    private CouponPreview preValidateCoupon(String code, Long userId, BigDecimal totalAmount) {
        if (code == null || code.isBlank()) {
            return CouponPreview.none(totalAmount);
        }
        try {
            ApiResponse<CouponValidationResponse> resp = couponClient.validate(
                    String.valueOf(userId), new ValidateCouponRequest(code, totalAmount));
            if (resp.isSuccess() && resp.getData() != null) {
                CouponValidationResponse data = resp.getData();
                log.debug("[SAGA] Coupon '{}' valid — discount={}, final={}", code, data.getDiscountAmount(), data.getFinalAmount());
                return CouponPreview.applied(data.getDiscountAmount(), data.getFinalAmount());
            }
        } catch (Exception ex) {
            log.warn("[SAGA] Coupon validation failed for code='{}': {}", code, ex.getMessage());
        }
        return CouponPreview.none(totalAmount);
    }

    private PaymentResponse callPayment(String orderNumber, Long userId,
                                        BigDecimal amount, CardRequest card) {
        PaymentCheckoutRequest req = new PaymentCheckoutRequest();
        req.setOrderId(orderNumber);
        req.setUserId(userId);
        req.setAmount(amount);
        req.setCurrency("TRY");
        req.setCardHolderName(card.getCardHolderName());
        req.setCardNumber(card.getCardNumber());
        req.setExpireMonth(card.getExpireMonth());
        req.setExpireYear(card.getExpireYear());
        req.setCvc(card.getCvc());

        ApiResponse<PaymentResponse> resp = paymentClient.checkout(req);
        if (resp == null || resp.getData() == null) {
            PaymentResponse declined = new PaymentResponse();
            declined.setStatus("FAILED");
            declined.setFailReason("Ödeme servisi geçersiz yanıt döndürdü.");
            return declined;
        }
        return resp.getData();
    }

    // Compensation step: release only successfully reserved items.
    // Never throws, only logs.
    private void releaseReservedStock(List<ResolvedItem> reservedItems, String orderNumber) {
        for (ResolvedItem item : reservedItems) {
            try {
                stockClient.releaseVariant(item.variantId(),
                        new StockReserveRequest(item.quantity(), "release:order:" + orderNumber));
                log.debug("[SAGA][COMPENSATION] Stock released — variantId={}, qty={}, order={}",
                        item.variantId(), item.quantity(), orderNumber);
            } catch (Exception ex) {
                log.error("[SAGA][COMPENSATION] Stock release FAILED for variantId={}, order={}: {}. " +
                        "Manual stock correction may be required.",
                        item.variantId(), orderNumber, ex.getMessage());
            }
        }
    }

    // Confirm reserved stock after payment capture.
    // Best-effort: if this fails, order still continues.
    private void confirmReservedStock(List<ResolvedItem> reservedItems, String orderNumber) {
        for (ResolvedItem item : reservedItems) {
            try {
                stockClient.confirmVariant(item.variantId(),
                        new StockReserveRequest(item.quantity(), "confirm:order:" + orderNumber));
                log.debug("[SAGA] Stock confirmed — variantId={}, qty={}, order={}",
                        item.variantId(), item.quantity(), orderNumber);
            } catch (Exception ex) {
                log.warn("[SAGA][BEST-EFFORT] Stock confirmation failed for variantId={}, order={}: {}. " +
                        "Stock may remain in 'reserved' state.",
                        item.variantId(), orderNumber, ex.getMessage());
            }
        }
    }

    // Mark coupon as used after payment.
    // Best-effort: if this fails, order still continues.
    private void consumeCoupon(String code, Long userId, String orderNumber, BigDecimal totalAmount) {
        try {
            couponClient.use(String.valueOf(userId),
                    new UseCouponRequest(code, orderNumber, totalAmount));
            log.debug("[SAGA] Coupon '{}' consumed for order={}", code, orderNumber);
        } catch (Exception ex) {
            log.warn("[SAGA][BEST-EFFORT] Coupon consumption failed for code='{}', order={}: {}. " +
                    "Coupon may remain usable.", code, orderNumber, ex.getMessage());
        }
    }

    private static BigDecimal computeTotal(List<ResolvedItem> items) {
        return items.stream()
                .map(i -> i.unitPrice().multiply(BigDecimal.valueOf(i.quantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private Order buildOrder(Long userId, String userEmail,
                             List<ResolvedItem> resolvedItems,
                             BigDecimal totalAmount,
                             BigDecimal discountAmount,
                             BigDecimal finalAmount) {
        Order order = new Order();
        order.setOrderNumber(generateOrderNumber());
        order.setUserId(userId);
        order.setUserEmail(userEmail);
        order.setTotalAmount(totalAmount);
        order.setDiscountAmount(discountAmount != null ? discountAmount : BigDecimal.ZERO);
        order.setFinalAmount(finalAmount != null ? finalAmount : totalAmount);
        order.setStatus(OrderStatus.PENDING);

        List<OrderItem> items = resolvedItems.stream().map(ri -> {
            OrderItem item = new OrderItem();
            item.setProductId(ri.productId());
            item.setVariantId(ri.variantId());
            item.setProductName(ri.productName());
            item.setUnitPrice(ri.unitPrice());
            item.setQuantity(ri.quantity());
            item.setLineTotal(ri.unitPrice().multiply(BigDecimal.valueOf(ri.quantity())));
            item.setOrder(order);
            return item;
        }).collect(Collectors.toList());

        order.setItems(items);
        return order;
    }

    private static String generateOrderNumber() {
        return "ORD-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
    }

    // Convert low-level stock errors into user-friendly messages.
    private static String friendlyStockError(Exception ex) {
        String msg = ex.getMessage();
        if (msg != null) {
            String lower = msg.toLowerCase();
            if (lower.contains("insufficient") || lower.contains("yetersiz")
                    || lower.contains("out of stock") || lower.contains("stok")) {
                return "Yeterli stok bulunmamaktadır.";
            }
        }
        return "Stok rezervasyonu sırasında bir hata oluştu. Lütfen tekrar deneyin.";
    }

    // -- mapping ---------------------------------------------------------------

    static OrderResponse toResponse(Order order) {
        OrderResponse resp = new OrderResponse();
        resp.setId(order.getId());
        resp.setOrderNumber(order.getOrderNumber());
        resp.setUserId(order.getUserId());
        resp.setUserEmail(order.getUserEmail());
        resp.setStatus(order.getStatus());
        resp.setTotalAmount(order.getTotalAmount());
        resp.setDiscountAmount(order.getDiscountAmount());
        resp.setFinalAmount(order.getFinalAmount());
        resp.setCouponCode(order.getCouponCode());
        resp.setFailReason(order.getFailReason());
        resp.setCreatedAt(order.getCreatedAt());
        resp.setUpdatedAt(order.getUpdatedAt());

        if (order.getItems() != null) {
            resp.setItems(order.getItems().stream().map(item -> {
                OrderItemResponse ir = new OrderItemResponse();
                ir.setId(item.getId());
                ir.setProductId(item.getProductId());
                ir.setVariantId(item.getVariantId());
                ir.setProductName(item.getProductName());
                ir.setUnitPrice(item.getUnitPrice());
                ir.setQuantity(item.getQuantity());
                ir.setLineTotal(item.getLineTotal());
                return ir;
            }).collect(Collectors.toList()));
        }

        return resp;
    }

    // -- internal value objects ------------------------------------------------

    private record ResolvedItem(Long productId, Long variantId, String productName,
                                BigDecimal unitPrice, int quantity) {}

    private record CouponPreview(BigDecimal discountAmount, BigDecimal finalAmount, boolean isApplied) {

        static CouponPreview none(BigDecimal totalAmount) {
            return new CouponPreview(BigDecimal.ZERO, totalAmount, false);
        }

        static CouponPreview applied(BigDecimal discount, BigDecimal finalAmt) {
            return new CouponPreview(discount, finalAmt, true);
        }
    }
}
