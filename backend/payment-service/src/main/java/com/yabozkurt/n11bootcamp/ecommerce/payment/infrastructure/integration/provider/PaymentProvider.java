package com.yabozkurt.n11bootcamp.ecommerce.payment.infrastructure.integration.provider;

import com.yabozkurt.n11bootcamp.ecommerce.payment.infrastructure.integration.provider.dto.PaymentProviderRequest;
import com.yabozkurt.n11bootcamp.ecommerce.payment.infrastructure.integration.provider.dto.PaymentProviderResult;

public interface PaymentProvider {
    String name();
    PaymentProviderResult pay(PaymentProviderRequest request);
    PaymentProviderResult refund(String providerPaymentId);
}
