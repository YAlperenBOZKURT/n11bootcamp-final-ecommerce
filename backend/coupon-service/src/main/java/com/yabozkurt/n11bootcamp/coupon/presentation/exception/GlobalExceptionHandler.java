package com.yabozkurt.n11bootcamp.coupon.presentation.exception;

import com.yabozkurt.n11bootcamp.coupon.domain.exception.CouponNotFoundException;
import com.yabozkurt.n11bootcamp.coupon.domain.exception.CouponValidationException;
import com.yabozkurt.n11bootcamp.coupon.presentation.dto.response.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(CouponNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleCouponNotFound(CouponNotFoundException ex) {
        log.warn("Coupon not found: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(CouponValidationException.class)
    public ResponseEntity<ApiResponse<Void>> handleCouponValidation(CouponValidationException ex) {
        log.warn("Coupon validation failed: {}", ex.getMessage());
        String message = ex.getMessage();
        HttpStatus status = message != null && message.contains("ADMIN rolü gerekli")
                ? HttpStatus.FORBIDDEN
                : HttpStatus.BAD_REQUEST;
        return ResponseEntity.status(status).body(ApiResponse.error(ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException ex) {
        String errors = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .collect(Collectors.joining(", "));
        log.warn("Validation failed: {}", errors);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error(errors));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleGeneral(Exception ex) {
        log.error("Unexpected error", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("Beklenmedik bir hata oluştu"));
    }
}
