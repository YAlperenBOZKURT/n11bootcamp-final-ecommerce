package com.yabozkurt.n11bootcamp.ecommerce.payment.infrastructure.integration.provider;

import com.yabozkurt.n11bootcamp.ecommerce.payment.domain.exception.PaymentValidationException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class PaymentProviderFactory {

    private final List<PaymentProvider> providers;
    private final String configuredProvider;

    public PaymentProviderFactory(List<PaymentProvider> providers,
                                  @Value("${payment.provider:fake}") String configuredProvider) {
        this.providers = providers;
        this.configuredProvider = configuredProvider;
    }

    public PaymentProvider getProvider() {
        return providers.stream()
                .filter(p -> p.name().equalsIgnoreCase(configuredProvider))
                .findFirst()
                .orElseThrow(() -> new PaymentValidationException("Payment provider bulunamadı: " + configuredProvider));
    }
}
