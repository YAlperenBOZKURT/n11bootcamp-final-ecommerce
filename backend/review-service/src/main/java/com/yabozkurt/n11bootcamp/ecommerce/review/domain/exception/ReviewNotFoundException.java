package com.yabozkurt.n11bootcamp.ecommerce.review.domain.exception;

public class ReviewNotFoundException extends RuntimeException {

    public ReviewNotFoundException(Long reviewId) {
        super("Review not found: " + reviewId);
    }
}

