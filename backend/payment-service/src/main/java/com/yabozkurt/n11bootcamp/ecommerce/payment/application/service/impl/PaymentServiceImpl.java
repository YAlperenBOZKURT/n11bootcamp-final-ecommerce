package com.yabozkurt.n11bootcamp.ecommerce.payment.application.service.impl;

import com.yabozkurt.n11bootcamp.ecommerce.payment.application.service.PaymentService;
import com.yabozkurt.n11bootcamp.ecommerce.payment.domain.exception.PaymentNotFoundException;
import com.yabozkurt.n11bootcamp.ecommerce.payment.domain.exception.PaymentValidationException;
import com.yabozkurt.n11bootcamp.ecommerce.payment.domain.model.PaymentTransaction;
import com.yabozkurt.n11bootcamp.ecommerce.payment.domain.model.enums.PaymentStatus;
import com.yabozkurt.n11bootcamp.ecommerce.payment.domain.repository.PaymentTransactionRepository;
import com.yabozkurt.n11bootcamp.ecommerce.payment.infrastructure.integration.provider.PaymentProvider;
import com.yabozkurt.n11bootcamp.ecommerce.payment.infrastructure.integration.provider.PaymentProviderFactory;
import com.yabozkurt.n11bootcamp.ecommerce.payment.infrastructure.integration.provider.dto.PaymentProviderRequest;
import com.yabozkurt.n11bootcamp.ecommerce.payment.infrastructure.integration.provider.dto.PaymentProviderResult;
import com.yabozkurt.n11bootcamp.ecommerce.payment.infrastructure.messaging.event.PaymentCompletedEvent;
import com.yabozkurt.n11bootcamp.ecommerce.payment.infrastructure.messaging.event.PaymentFailedEvent;
import com.yabozkurt.n11bootcamp.ecommerce.payment.infrastructure.messaging.publisher.PaymentEventPublisher;
import com.yabozkurt.n11bootcamp.ecommerce.payment.presentation.dto.request.PaymentCheckoutRequest;
import com.yabozkurt.n11bootcamp.ecommerce.payment.presentation.dto.request.RefundRequest;
import com.yabozkurt.n11bootcamp.ecommerce.payment.presentation.dto.response.PaymentResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class PaymentServiceImpl implements PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentServiceImpl.class);

    private final PaymentTransactionRepository repository;
    private final PaymentProviderFactory providerFactory;
    private final PaymentEventPublisher eventPublisher;

    public PaymentServiceImpl(PaymentTransactionRepository repository,
                              PaymentProviderFactory providerFactory,
                              PaymentEventPublisher eventPublisher) {
        this.repository = repository;
        this.providerFactory = providerFactory;
        this.eventPublisher = eventPublisher;
    }

    @Override
    @Transactional
    public PaymentResponse checkout(PaymentCheckoutRequest request) {
        log.info("Payment checkout initiated: orderId={}, amount={} {}", request.getOrderId(), request.getAmount(), request.getCurrency());
        PaymentTransaction existing = repository.findByOrderId(request.getOrderId()).orElse(null);
        if (existing != null) {
            log.info("Duplicate payment request ignored, returning existing transaction: orderId={}, status={}", request.getOrderId(), existing.getStatus());
            return toResponse(existing);
        }

        PaymentProvider provider = providerFactory.getProvider();
        PaymentProviderRequest providerRequest = new PaymentProviderRequest();
        providerRequest.setOrderId(request.getOrderId());
        providerRequest.setAmount(request.getAmount());
        providerRequest.setCurrency(request.getCurrency());
        providerRequest.setCardHolderName(request.getCardHolderName());
        providerRequest.setCardNumber(request.getCardNumber());
        providerRequest.setExpireMonth(request.getExpireMonth());
        providerRequest.setExpireYear(request.getExpireYear());
        providerRequest.setCvc(request.getCvc());

        PaymentProviderResult result = provider.pay(providerRequest);

        PaymentTransaction tx = new PaymentTransaction();
        tx.setOrderId(request.getOrderId());
        tx.setUserId(request.getUserId());
        tx.setAmount(request.getAmount());
        tx.setCurrency(request.getCurrency());
        tx.setProvider(provider.name());
        tx.setProviderPaymentId(result.getProviderPaymentId());
        tx.setStatus(result.isSuccess() ? PaymentStatus.SUCCESS : PaymentStatus.FAILED);
        tx.setFailReason(result.getFailReason());

        PaymentTransaction saved = repository.save(tx);

        if (result.isSuccess()) {
            log.info("Payment succeeded: orderId={}, provider={}, providerPaymentId={}", saved.getOrderId(), saved.getProvider(), saved.getProviderPaymentId());
            eventPublisher.publishPaymentCompleted(new PaymentCompletedEvent(
                    saved.getOrderId(), saved.getUserId(), saved.getAmount(),
                    saved.getProvider(), saved.getProviderPaymentId()));
        } else {
            log.warn("Payment failed: orderId={}, reason={}", saved.getOrderId(), saved.getFailReason());
            eventPublisher.publishPaymentFailed(new PaymentFailedEvent(
                    saved.getOrderId(), saved.getUserId(), saved.getAmount(), saved.getFailReason()));
        }

        return toResponse(saved);
    }

    @Override
    @Transactional
    public PaymentResponse refund(RefundRequest request) {
        PaymentTransaction tx = repository.findByOrderId(request.getOrderId())
                .orElseThrow(() -> new PaymentNotFoundException(request.getOrderId()));

        log.info("Refund requested: orderId={}, currentStatus={}", request.getOrderId(), tx.getStatus());
        if (tx.getStatus() == PaymentStatus.REFUNDED) {
            return toResponse(tx);
        }
        if (tx.getStatus() != PaymentStatus.SUCCESS) {
            throw new PaymentValidationException("Sadece başarılı ödemeler iade edilebilir");
        }

        PaymentProvider provider = providerFactory.getProvider();
        PaymentProviderResult result = provider.refund(tx.getProviderPaymentId());
        if (!result.isSuccess()) {
            log.warn("Refund failed via provider: orderId={}, reason={}", request.getOrderId(), result.getFailReason());
            throw new PaymentValidationException(result.getFailReason());
        }

        tx.setStatus(PaymentStatus.REFUNDED);
        log.info("Refund completed: orderId={}, providerPaymentId={}", request.getOrderId(), tx.getProviderPaymentId());
        return toResponse(repository.save(tx));
    }

    @Override
    public PaymentResponse getByOrderId(String orderId) {
        return toResponse(repository.findByOrderId(orderId).orElseThrow(() -> new PaymentNotFoundException(orderId)));
    }

    private static PaymentResponse toResponse(PaymentTransaction tx) {
        PaymentResponse response = new PaymentResponse();
        response.setId(tx.getId());
        response.setOrderId(tx.getOrderId());
        response.setUserId(tx.getUserId());
        response.setAmount(tx.getAmount());
        response.setCurrency(tx.getCurrency());
        response.setStatus(tx.getStatus());
        response.setProvider(tx.getProvider());
        response.setProviderPaymentId(tx.getProviderPaymentId());
        response.setFailReason(tx.getFailReason());
        response.setCreatedAt(tx.getCreatedAt());
        response.setUpdatedAt(tx.getUpdatedAt());
        return response;
    }
}
