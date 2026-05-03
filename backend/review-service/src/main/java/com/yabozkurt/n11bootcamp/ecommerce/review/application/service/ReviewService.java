package com.yabozkurt.n11bootcamp.ecommerce.review.application.service;

import com.yabozkurt.n11bootcamp.ecommerce.review.presentation.dto.request.CreateReviewRequest;
import com.yabozkurt.n11bootcamp.ecommerce.review.presentation.dto.request.UpdateReviewRequest;
import com.yabozkurt.n11bootcamp.ecommerce.review.presentation.dto.response.ReviewResponse;

import java.util.List;

public interface ReviewService {

    ReviewResponse create(Long userId, CreateReviewRequest request);

    ReviewResponse getById(Long id);

    List<ReviewResponse> getByProductId(Long productId);

    List<ReviewResponse> getByUserId(Long userId);

    ReviewResponse update(Long userId, Long id, UpdateReviewRequest request);

    void delete(Long userId, Long id);

    void deleteByAdmin(Long id);
}
