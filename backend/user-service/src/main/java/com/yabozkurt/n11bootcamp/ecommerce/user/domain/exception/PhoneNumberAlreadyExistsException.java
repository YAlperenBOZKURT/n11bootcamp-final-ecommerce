package com.yabozkurt.n11bootcamp.ecommerce.user.domain.exception;

public class PhoneNumberAlreadyExistsException extends RuntimeException {

    public PhoneNumberAlreadyExistsException(String phoneNumber) {
        super("User already exists with phone number: " + phoneNumber);
    }
}
