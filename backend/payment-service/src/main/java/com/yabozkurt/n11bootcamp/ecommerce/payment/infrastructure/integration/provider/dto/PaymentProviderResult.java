package com.yabozkurt.n11bootcamp.ecommerce.payment.infrastructure.integration.provider.dto;

public class PaymentProviderResult {
    private boolean success;
    private String providerPaymentId;
    private String failReason;

    public static PaymentProviderResult success(String providerPaymentId) {
        PaymentProviderResult result = new PaymentProviderResult();
        result.success = true;
        result.providerPaymentId = providerPaymentId;
        return result;
    }

    public static PaymentProviderResult fail(String failReason) {
        PaymentProviderResult result = new PaymentProviderResult();
        result.success = false;
        result.failReason = failReason;
        return result;
    }

    public boolean isSuccess() { return success; }
    public String getProviderPaymentId() { return providerPaymentId; }
    public String getFailReason() { return failReason; }
}
