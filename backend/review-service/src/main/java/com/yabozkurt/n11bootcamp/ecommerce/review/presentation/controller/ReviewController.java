package com.yabozkurt.n11bootcamp.ecommerce.review.presentation.controller;

import com.yabozkurt.n11bootcamp.ecommerce.review.application.service.ReviewService;
import com.yabozkurt.n11bootcamp.ecommerce.review.presentation.dto.request.CreateReviewRequest;
import com.yabozkurt.n11bootcamp.ecommerce.review.presentation.dto.request.UpdateReviewRequest;
import com.yabozkurt.n11bootcamp.ecommerce.review.presentation.dto.response.ApiResponse;
import com.yabozkurt.n11bootcamp.ecommerce.review.presentation.dto.response.ReviewResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/reviews")
@Tag(name = "Reviews", description = "Review CRUD operations")
@SecurityRequirement(name = "cookieAuth")
public class ReviewController {

    private final ReviewService reviewService;

    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @Operation(summary = "Create review")
    @PostMapping
    public ResponseEntity<ApiResponse<ReviewResponse>> create(
            @RequestHeader(name = "X-User-Id", required = false) String userIdHeader,
            @Valid @RequestBody CreateReviewRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(reviewService.create(parseUserId(userIdHeader), request)));
    }

    @Operation(summary = "Get review by id")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ReviewResponse>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(reviewService.getById(id)));
    }

    @Operation(summary = "List active reviews by product")
    @GetMapping("/products/{productId}")
    public ResponseEntity<ApiResponse<List<ReviewResponse>>> getByProductId(@PathVariable Long productId) {
        return ResponseEntity.ok(ApiResponse.ok(reviewService.getByProductId(productId)));
    }

    @Operation(summary = "List active reviews by user")
    @GetMapping("/users/{userId}")
    public ResponseEntity<ApiResponse<List<ReviewResponse>>> getByUserId(@PathVariable Long userId) {
        return ResponseEntity.ok(ApiResponse.ok(reviewService.getByUserId(userId)));
    }

    @Operation(summary = "Update review")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ReviewResponse>> update(
            @RequestHeader(name = "X-User-Id", required = false) String userIdHeader,
            @PathVariable Long id,
            @Valid @RequestBody UpdateReviewRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(reviewService.update(parseUserId(userIdHeader), id, request)));
    }

    @Operation(summary = "Delete review (soft delete)")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @RequestHeader(name = "X-User-Id", required = false) String userIdHeader,
            @PathVariable Long id) {
        reviewService.delete(parseUserId(userIdHeader), id);
        return ResponseEntity.ok(ApiResponse.ok("Review deleted", null));
    }

    @Operation(summary = "Delete any review (ADMIN)")
    @DeleteMapping("/admin/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteByAdmin(@PathVariable Long id) {
        reviewService.deleteByAdmin(id);
        return ResponseEntity.ok(ApiResponse.ok("Review deleted by admin", null));
    }

    private Long parseUserId(String userIdHeader) {
        if (userIdHeader == null || userIdHeader.isBlank()) {
            throw new IllegalArgumentException("X-User-Id header is required");
        }
        try {
            return Long.valueOf(userIdHeader);
        } catch (NumberFormatException ex) {
            throw new IllegalArgumentException("X-User-Id header is invalid");
        }
    }
}
