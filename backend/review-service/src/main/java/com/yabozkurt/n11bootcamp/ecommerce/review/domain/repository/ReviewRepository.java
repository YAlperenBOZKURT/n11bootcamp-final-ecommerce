package com.yabozkurt.n11bootcamp.ecommerce.review.domain.repository;

import com.yabozkurt.n11bootcamp.ecommerce.review.domain.model.Review;
import com.yabozkurt.n11bootcamp.ecommerce.review.domain.model.enums.ReviewStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    Optional<Review> findByIdAndStatus(Long id, ReviewStatus status);

    List<Review> findByProductIdAndStatusOrderByCreatedAtDesc(Long productId, ReviewStatus status);

    List<Review> findByUserIdAndStatusOrderByCreatedAtDesc(Long userId, ReviewStatus status);
}

