package com.yabozkurt.n11bootcamp.ecommerce.order.presentation.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class CardRequest {

    @NotBlank
    private String cardHolderName;

    @NotBlank
    @Size(min = 16, max = 16)
    @Pattern(regexp = "\\d{16}")
    private String cardNumber;

    @NotBlank
    @Pattern(regexp = "0[1-9]|1[0-2]")
    private String expireMonth;

    @NotBlank
    @Pattern(regexp = "\\d{4}")
    private String expireYear;

    @NotBlank
    @Size(min = 3, max = 4)
    @Pattern(regexp = "\\d{3,4}")
    private String cvc;

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
