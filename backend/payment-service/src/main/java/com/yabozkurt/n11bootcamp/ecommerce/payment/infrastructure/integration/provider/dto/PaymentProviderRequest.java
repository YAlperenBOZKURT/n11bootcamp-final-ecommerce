package com.yabozkurt.n11bootcamp.ecommerce.payment.infrastructure.integration.provider.dto;

import java.math.BigDecimal;

public class PaymentProviderRequest {
    private String orderId;
    private BigDecimal amount;
    private String currency;
    private String cardHolderName;
    private String cardNumber;
    private String expireMonth;
    private String expireYear;
    private String cvc;

    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public String getCardHolderName() { return cardHolderName; }
    public void setCardHolderName(String cardHolderName) { this.cardHolderName = cardHolderName; }
    public String getCardNumber() { return cardNumber; }
    public void setCardNumber(String cardNumber) { this.cardNumber = cardNumber; }
    public String getExpireMonth() { return expireMonth; }
    public void setExpireMonth(String expireMonth) { this.expireMonth = expireMonth; }
    public String getExpireYear() { return expireYear; }
    public void setExpireYear(String expireYear) { this.expireYear = expireYear; }
    public String getCvc() { return cvc; }
    public void setCvc(String cvc) { this.cvc = cvc; }
}
