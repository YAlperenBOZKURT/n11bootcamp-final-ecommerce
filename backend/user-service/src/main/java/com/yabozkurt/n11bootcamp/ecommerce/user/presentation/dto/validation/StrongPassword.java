package com.yabozkurt.n11bootcamp.ecommerce.user.presentation.dto.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = StrongPasswordValidator.class)
public @interface StrongPassword {

    String message() default "Şifre en az 8 karakter içermeli; en az bir büyük harf, bir rakam ve bir özel karakter bulunmalıdır.";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
