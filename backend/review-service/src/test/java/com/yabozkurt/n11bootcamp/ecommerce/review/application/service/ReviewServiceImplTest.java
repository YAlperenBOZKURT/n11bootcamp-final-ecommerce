package com.yabozkurt.n11bootcamp.ecommerce.review.application.service;

import com.yabozkurt.n11bootcamp.ecommerce.review.application.service.impl.ReviewServiceImpl;
import com.yabozkurt.n11bootcamp.ecommerce.review.domain.exception.ReviewNotFoundException;
import com.yabozkurt.n11bootcamp.ecommerce.review.domain.model.Review;
import com.yabozkurt.n11bootcamp.ecommerce.review.domain.model.enums.ReviewStatus;
import com.yabozkurt.n11bootcamp.ecommerce.review.domain.repository.ReviewRepository;
import com.yabozkurt.n11bootcamp.ecommerce.review.presentation.dto.request.CreateReviewRequest;
import com.yabozkurt.n11bootcamp.ecommerce.review.presentation.dto.request.UpdateReviewRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReviewServiceImplTest {

    @Mock
    private ReviewRepository reviewRepository;

    private ReviewServiceImpl reviewService;

    @BeforeEach
    void setUp() {
        reviewService = new ReviewServiceImpl(reviewRepository);
    }

    @Test
    void create_savesActiveReview() {
        CreateReviewRequest request = new CreateReviewRequest();
        request.setProductId(10L);
        request.setRating(5);
        request.setCommentText("Great product");

        Review saved = new Review();
        saved.setUserId(7L);
        saved.setProductId(10L);
        saved.setRating(5);
        saved.setCommentText("Great product");
        saved.setStatus(ReviewStatus.ACTIVE);

        when(reviewRepository.save(any(Review.class))).thenReturn(saved);

        reviewService.create(7L, request);

        ArgumentCaptor<Review> captor = ArgumentCaptor.forClass(Review.class);
        verify(reviewRepository).save(captor.capture());
        Review persisted = captor.getValue();
        assertThat(persisted.getUserId()).isEqualTo(7L);
        assertThat(persisted.getProductId()).isEqualTo(10L);
        assertThat(persisted.getRating()).isEqualTo(5);
        assertThat(persisted.getCommentText()).isEqualTo("Great product");
        assertThat(persisted.getStatus()).isEqualTo(ReviewStatus.ACTIVE);
    }

    @Test
    void delete_ownerMismatch_throwsForbiddenState() {
        Review review = new Review();
        review.setUserId(99L);
        review.setStatus(ReviewStatus.ACTIVE);

        when(reviewRepository.findByIdAndStatus(1L, ReviewStatus.ACTIVE)).thenReturn(Optional.of(review));

        assertThatThrownBy(() -> reviewService.delete(10L, 1L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("own review");
    }

    @Test
    void delete_owner_softDeletesReview() {
        Review review = new Review();
        review.setUserId(10L);
        review.setStatus(ReviewStatus.ACTIVE);

        when(reviewRepository.findByIdAndStatus(3L, ReviewStatus.ACTIVE)).thenReturn(Optional.of(review));
        when(reviewRepository.save(any(Review.class))).thenAnswer(inv -> inv.getArgument(0));

        reviewService.delete(10L, 3L);

        ArgumentCaptor<Review> captor = ArgumentCaptor.forClass(Review.class);
        verify(reviewRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(ReviewStatus.DELETED);
    }

    @Test
    void deleteByAdmin_softDeletesWithoutOwnershipCheck() {
        Review review = new Review();
        review.setUserId(999L);
        review.setStatus(ReviewStatus.ACTIVE);

        when(reviewRepository.findByIdAndStatus(5L, ReviewStatus.ACTIVE)).thenReturn(Optional.of(review));
        when(reviewRepository.save(any(Review.class))).thenAnswer(inv -> inv.getArgument(0));

        reviewService.deleteByAdmin(5L);

        ArgumentCaptor<Review> captor = ArgumentCaptor.forClass(Review.class);
        verify(reviewRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(ReviewStatus.DELETED);
    }

    @Test
    void deleteByAdmin_missingReview_throwsNotFound() {
        when(reviewRepository.findByIdAndStatus(404L, ReviewStatus.ACTIVE)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reviewService.deleteByAdmin(404L))
                .isInstanceOf(ReviewNotFoundException.class)
                .hasMessageContaining("404");
    }

    @Test
    void update_owner_updatesRatingAndText() {
        Review review = new Review();
        review.setUserId(77L);
        review.setRating(2);
        review.setCommentText("old");
        review.setStatus(ReviewStatus.ACTIVE);

        UpdateReviewRequest request = new UpdateReviewRequest();
        request.setRating(4);
        request.setCommentText("new text");

        when(reviewRepository.findByIdAndStatus(11L, ReviewStatus.ACTIVE)).thenReturn(Optional.of(review));
        when(reviewRepository.save(any(Review.class))).thenAnswer(inv -> inv.getArgument(0));

        reviewService.update(77L, 11L, request);

        ArgumentCaptor<Review> captor = ArgumentCaptor.forClass(Review.class);
        verify(reviewRepository).save(captor.capture());
        assertThat(captor.getValue().getRating()).isEqualTo(4);
        assertThat(captor.getValue().getCommentText()).isEqualTo("new text");
    }
}

