package com.yabozkurt.n11bootcamp.ecommerce.payment.presentation.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public class PaymentCheckoutRequest {
    @NotBlank
    private String orderId;
    @NotNull
    private Long userId;
    @NotNull
    @DecimalMin("0.01")
    private BigDecimal amount;
    @NotBlank
    private String currency;
    @NotBlank
    private String cardHolderName;
    @NotBlank
    private String cardNumber;
    @NotBlank
    private String expireMonth;
    @NotBlank
    private String expireYear;
    @NotBlank
    private String cvc;

    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
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
