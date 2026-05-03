package com.yabozkurt.n11bootcamp.ecommerce.review.presentation.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yabozkurt.n11bootcamp.ecommerce.review.application.service.ReviewService;
import com.yabozkurt.n11bootcamp.ecommerce.review.presentation.dto.request.CreateReviewRequest;
import com.yabozkurt.n11bootcamp.ecommerce.review.presentation.dto.request.UpdateReviewRequest;
import com.yabozkurt.n11bootcamp.ecommerce.review.presentation.dto.response.ReviewResponse;
import com.yabozkurt.n11bootcamp.ecommerce.review.presentation.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ReviewControllerTest {

    private ReviewService reviewService;
    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        reviewService = mock(ReviewService.class);
        ReviewController controller = new ReviewController(reviewService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        objectMapper = new ObjectMapper();
    }

    @Test
    void deleteByAdmin_callsServiceAndReturns200() throws Exception {
        mockMvc.perform(delete("/api/reviews/admin/15"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Review deleted by admin"));

        verify(reviewService).deleteByAdmin(15L);
    }

    @Test
    void delete_ownerDeleteWithoutHeader_returns400() throws Exception {
        mockMvc.perform(delete("/api/reviews/11"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));

        verify(reviewService, never()).delete(any(), any());
    }

    @Test
    void create_validPayload_returns201() throws Exception {
        CreateReviewRequest request = new CreateReviewRequest();
        request.setProductId(5L);
        request.setRating(4);
        request.setCommentText("good");

        ReviewResponse response = new ReviewResponse();
        response.setId(1L);
        response.setProductId(5L);
        response.setUserId(7L);
        response.setRating(4);
        response.setCommentText("good");

        when(reviewService.create(eq(7L), any(CreateReviewRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/reviews")
                        .header("X-User-Id", "7")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.productId").value(5))
                .andExpect(jsonPath("$.data.userId").value(7));
    }

    @Test
    void getByProduct_returnsList() throws Exception {
        ReviewResponse r = new ReviewResponse();
        r.setId(1L);
        r.setProductId(8L);
        r.setUserId(3L);
        r.setRating(5);
        r.setCommentText("perfect");

        when(reviewService.getByProductId(8L)).thenReturn(List.of(r));

        mockMvc.perform(get("/api/reviews/products/8"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].productId").value(8));
    }

    @Test
    void update_callsService() throws Exception {
        UpdateReviewRequest request = new UpdateReviewRequest();
        request.setRating(3);
        request.setCommentText("updated");

        ReviewResponse response = new ReviewResponse();
        response.setId(22L);
        response.setProductId(4L);
        response.setUserId(9L);
        response.setRating(3);
        response.setCommentText("updated");

        when(reviewService.update(eq(9L), eq(22L), any(UpdateReviewRequest.class))).thenReturn(response);

        mockMvc.perform(put("/api/reviews/22")
                        .header("X-User-Id", "9")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(22))
                .andExpect(jsonPath("$.data.rating").value(3));
    }
}

