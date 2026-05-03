package com.yabozkurt.n11bootcamp.ecommerce.review.application.service.impl;

import com.yabozkurt.n11bootcamp.ecommerce.review.application.service.ReviewService;
import com.yabozkurt.n11bootcamp.ecommerce.review.domain.exception.ReviewNotFoundException;
import com.yabozkurt.n11bootcamp.ecommerce.review.domain.model.Review;
import com.yabozkurt.n11bootcamp.ecommerce.review.domain.model.enums.ReviewStatus;
import com.yabozkurt.n11bootcamp.ecommerce.review.domain.repository.ReviewRepository;
import com.yabozkurt.n11bootcamp.ecommerce.review.presentation.dto.request.CreateReviewRequest;
import com.yabozkurt.n11bootcamp.ecommerce.review.presentation.dto.request.UpdateReviewRequest;
import com.yabozkurt.n11bootcamp.ecommerce.review.presentation.dto.response.ReviewResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class ReviewServiceImpl implements ReviewService {

    private static final Logger log = LoggerFactory.getLogger(ReviewServiceImpl.class);

    private final ReviewRepository reviewRepository;

    public ReviewServiceImpl(ReviewRepository reviewRepository) {
        this.reviewRepository = reviewRepository;
    }

    @Override
    @Transactional
    public ReviewResponse create(Long userId, CreateReviewRequest request) {
        Review review = new Review();
        review.setUserId(userId);
        review.setProductId(request.getProductId());
        review.setRating(request.getRating());
        review.setCommentText(request.getCommentText());
        review.setStatus(ReviewStatus.ACTIVE);
        ReviewResponse response = toResponse(reviewRepository.save(review));
        log.info("Review created: id={}, userId={}, productId={}, rating={}", response.getId(), userId, request.getProductId(), request.getRating());
        return response;
    }

    @Override
    public ReviewResponse getById(Long id) {
        return toResponse(findActiveById(id));
    }

    @Override
    public List<ReviewResponse> getByProductId(Long productId) {
        return reviewRepository.findByProductIdAndStatusOrderByCreatedAtDesc(productId, ReviewStatus.ACTIVE)
                .stream()
                .map(ReviewServiceImpl::toResponse)
                .toList();
    }

    @Override
    public List<ReviewResponse> getByUserId(Long userId) {
        return reviewRepository.findByUserIdAndStatusOrderByCreatedAtDesc(userId, ReviewStatus.ACTIVE)
                .stream()
                .map(ReviewServiceImpl::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public ReviewResponse update(Long userId, Long id, UpdateReviewRequest request) {
        Review review = findActiveById(id);
        ensureOwner(userId, review);

        review.setRating(request.getRating());
        review.setCommentText(request.getCommentText());
        return toResponse(reviewRepository.save(review));
    }

    @Override
    @Transactional
    public void delete(Long userId, Long id) {
        Review review = findActiveById(id);
        ensureOwner(userId, review);

        review.setStatus(ReviewStatus.DELETED);
        reviewRepository.save(review);
        log.info("Review deleted by owner: id={}, userId={}", id, userId);
    }

    @Override
    @Transactional
    public void deleteByAdmin(Long id) {
        Review review = findActiveById(id);
        review.setStatus(ReviewStatus.DELETED);
        reviewRepository.save(review);
        log.info("Review deleted by admin: id={}, productId={}", id, review.getProductId());
    }

    private Review findActiveById(Long id) {
        return reviewRepository.findByIdAndStatus(id, ReviewStatus.ACTIVE)
                .orElseThrow(() -> new ReviewNotFoundException(id));
    }

    private static void ensureOwner(Long userId, Review review) {
        if (!review.getUserId().equals(userId)) {
            throw new IllegalStateException("You can only modify your own review");
        }
    }

    public static ReviewResponse toResponse(Review review) {
        ReviewResponse response = new ReviewResponse();
        response.setId(review.getId());
        response.setProductId(review.getProductId());
        response.setUserId(review.getUserId());
        response.setRating(review.getRating());
        response.setCommentText(review.getCommentText());
        response.setCreatedAt(review.getCreatedAt());
        response.setUpdatedAt(review.getUpdatedAt());
        return response;
    }
}
