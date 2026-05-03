package com.yabozkurt.n11bootcamp.ecommerce.payment.infrastructure.integration.provider.fake;

import com.yabozkurt.n11bootcamp.ecommerce.payment.infrastructure.integration.provider.PaymentProvider;
import com.yabozkurt.n11bootcamp.ecommerce.payment.infrastructure.integration.provider.dto.PaymentProviderRequest;
import com.yabozkurt.n11bootcamp.ecommerce.payment.infrastructure.integration.provider.dto.PaymentProviderResult;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class FakePaymentProvider implements PaymentProvider {
    @Override
    public String name() {
        return "fake";
    }

    @Override
    public PaymentProviderResult pay(PaymentProviderRequest request) {
        if (request.getCardNumber() != null && request.getCardNumber().endsWith("0000")) {
            return PaymentProviderResult.fail("Kart reddedildi (fake)");
        }
        return PaymentProviderResult.success("fake-" + UUID.randomUUID());
    }

    @Override
    public PaymentProviderResult refund(String providerPaymentId) {
        return PaymentProviderResult.success(providerPaymentId);
    }
}
