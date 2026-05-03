package com.yabozkurt.n11bootcamp.ecommerce.order.infrastructure.client.dto;

public class PaymentResponse {
    private String status;
    private String failReason;

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getFailReason() { return failReason; }
    public void setFailReason(String failReason) { this.failReason = failReason; }

    public boolean isSuccess() { return "SUCCESS".equals(status); }
}
