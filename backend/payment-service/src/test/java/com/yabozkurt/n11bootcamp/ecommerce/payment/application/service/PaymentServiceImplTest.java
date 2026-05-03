package com.yabozkurt.n11bootcamp.ecommerce.payment.application.service;

import com.yabozkurt.n11bootcamp.ecommerce.payment.application.service.impl.PaymentServiceImpl;
import com.yabozkurt.n11bootcamp.ecommerce.payment.domain.exception.PaymentNotFoundException;
import com.yabozkurt.n11bootcamp.ecommerce.payment.domain.exception.PaymentValidationException;
import com.yabozkurt.n11bootcamp.ecommerce.payment.domain.model.PaymentTransaction;
import com.yabozkurt.n11bootcamp.ecommerce.payment.domain.model.enums.PaymentStatus;
import com.yabozkurt.n11bootcamp.ecommerce.payment.domain.repository.PaymentTransactionRepository;
import com.yabozkurt.n11bootcamp.ecommerce.payment.infrastructure.integration.provider.PaymentProvider;
import com.yabozkurt.n11bootcamp.ecommerce.payment.infrastructure.integration.provider.PaymentProviderFactory;
import com.yabozkurt.n11bootcamp.ecommerce.payment.infrastructure.integration.provider.dto.PaymentProviderResult;
import com.yabozkurt.n11bootcamp.ecommerce.payment.infrastructure.messaging.publisher.PaymentEventPublisher;
import com.yabozkurt.n11bootcamp.ecommerce.payment.presentation.dto.request.PaymentCheckoutRequest;
import com.yabozkurt.n11bootcamp.ecommerce.payment.presentation.dto.request.RefundRequest;
import com.yabozkurt.n11bootcamp.ecommerce.payment.presentation.dto.response.PaymentResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PaymentServiceImplTest {

    @Mock private PaymentTransactionRepository repository;
    @Mock private PaymentProviderFactory providerFactory;
    @Mock private PaymentProvider provider;
    @Mock private PaymentEventPublisher eventPublisher;

    private PaymentServiceImpl paymentService;

    @BeforeEach
    void setUp() {
        paymentService = new PaymentServiceImpl(repository, providerFactory, eventPublisher);
        when(providerFactory.getProvider()).thenReturn(provider);
        when(provider.name()).thenReturn("FAKE");
    }

    private PaymentCheckoutRequest checkoutRequest() {
        PaymentCheckoutRequest req = new PaymentCheckoutRequest();
        req.setOrderId("ORD-001");
        req.setUserId(1L);
        req.setAmount(new BigDecimal("150.00"));
        req.setCurrency("TRY");
        req.setCardHolderName("Test User");
        req.setCardNumber("4111111111111111");
        req.setExpireMonth("12");
        req.setExpireYear("2030");
        req.setCvc("123");
        return req;
    }

    private PaymentTransaction savedTx(PaymentStatus status) {
        PaymentTransaction tx = new PaymentTransaction();
        tx.setOrderId("ORD-001");
        tx.setUserId(1L);
        tx.setAmount(new BigDecimal("150.00"));
        tx.setCurrency("TRY");
        tx.setProvider("FAKE");
        tx.setProviderPaymentId("PAY-XYZ");
        tx.setStatus(status);
        return tx;
    }

    @Test
    void checkout_success_publishesCompletedEvent() {
        PaymentProviderResult result = PaymentProviderResult.success("PAY-XYZ");

        when(repository.findByOrderId("ORD-001")).thenReturn(Optional.empty());
        when(provider.pay(any())).thenReturn(result);
        when(repository.save(any())).thenReturn(savedTx(PaymentStatus.SUCCESS));

        PaymentResponse response = paymentService.checkout(checkoutRequest());

        assertThat(response.getStatus()).isEqualTo(PaymentStatus.SUCCESS);
        verify(eventPublisher).publishPaymentCompleted(any());
        verify(eventPublisher, never()).publishPaymentFailed(any());
    }

    @Test
    void checkout_providerFails_publishesFailedEvent() {
        PaymentProviderResult result = PaymentProviderResult.fail("Yetersiz limit");

        when(repository.findByOrderId("ORD-001")).thenReturn(Optional.empty());
        when(provider.pay(any())).thenReturn(result);
        when(repository.save(any())).thenReturn(savedTx(PaymentStatus.FAILED));

        PaymentResponse response = paymentService.checkout(checkoutRequest());

        assertThat(response.getStatus()).isEqualTo(PaymentStatus.FAILED);
        verify(eventPublisher).publishPaymentFailed(any());
        verify(eventPublisher, never()).publishPaymentCompleted(any());
    }

    @Test
    void checkout_duplicateOrder_returnsExisting() {
        PaymentTransaction existing = savedTx(PaymentStatus.SUCCESS);
        when(repository.findByOrderId("ORD-001")).thenReturn(Optional.of(existing));

        PaymentResponse response = paymentService.checkout(checkoutRequest());

        assertThat(response.getStatus()).isEqualTo(PaymentStatus.SUCCESS);
        verify(provider, never()).pay(any());
        verify(eventPublisher, never()).publishPaymentCompleted(any());
    }

    @Test
    void refund_success() {
        PaymentTransaction tx = savedTx(PaymentStatus.SUCCESS);
        PaymentProviderResult result = PaymentProviderResult.success("PAY-XYZ");

        when(repository.findByOrderId("ORD-001")).thenReturn(Optional.of(tx));
        when(provider.refund(any())).thenReturn(result);
        when(repository.save(any())).thenReturn(savedTx(PaymentStatus.REFUNDED));

        RefundRequest req = new RefundRequest();
        req.setOrderId("ORD-001");
        PaymentResponse response = paymentService.refund(req);

        assertThat(response.getStatus()).isEqualTo(PaymentStatus.REFUNDED);
    }

    @Test
    void refund_alreadyRefunded_returnsExisting() {
        PaymentTransaction tx = savedTx(PaymentStatus.REFUNDED);
        when(repository.findByOrderId("ORD-001")).thenReturn(Optional.of(tx));

        RefundRequest req = new RefundRequest();
        req.setOrderId("ORD-001");
        PaymentResponse response = paymentService.refund(req);

        assertThat(response.getStatus()).isEqualTo(PaymentStatus.REFUNDED);
        verify(provider, never()).refund(any());
    }

    @Test
    void refund_notSuccessStatus_throwsValidationException() {
        PaymentTransaction tx = savedTx(PaymentStatus.FAILED);
        when(repository.findByOrderId("ORD-001")).thenReturn(Optional.of(tx));

        RefundRequest req = new RefundRequest();
        req.setOrderId("ORD-001");

        assertThatThrownBy(() -> paymentService.refund(req))
                .isInstanceOf(PaymentValidationException.class);
    }

    @Test
    void refund_orderNotFound_throwsNotFoundException() {
        when(repository.findByOrderId("NOTEXIST")).thenReturn(Optional.empty());

        RefundRequest req = new RefundRequest();
        req.setOrderId("NOTEXIST");

        assertThatThrownBy(() -> paymentService.refund(req))
                .isInstanceOf(PaymentNotFoundException.class);
    }

    @Test
    void getByOrderId_found() {
        when(repository.findByOrderId("ORD-001")).thenReturn(Optional.of(savedTx(PaymentStatus.SUCCESS)));

        assertThat(paymentService.getByOrderId("ORD-001").getStatus()).isEqualTo(PaymentStatus.SUCCESS);
    }

    @Test
    void getByOrderId_notFound_throwsNotFoundException() {
        when(repository.findByOrderId("NOTEXIST")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> paymentService.getByOrderId("NOTEXIST"))
                .isInstanceOf(PaymentNotFoundException.class);
    }
}
