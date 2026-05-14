package com.medibook.review.resource;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.medibook.review.config.SecurityConfig;
import com.medibook.review.dto.request.ReviewRequest;
import com.medibook.review.entity.Review;
import com.medibook.review.exception.BadRequestException;
import com.medibook.review.exception.GlobalExceptionHandler;
import com.medibook.review.service.ReviewService;

@WebMvcTest(ReviewResource.class)
@AutoConfigureMockMvc(addFilters = true)
@Import({ SecurityConfig.class, GlobalExceptionHandler.class })
class ReviewResourceTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ReviewService reviewService;

    @Test
    void submitReviewReturnsCreatedReview() throws Exception {
        when(reviewService.submitReview(any(ReviewRequest.class))).thenReturn(createReview());

        mockMvc.perform(post("/reviews/submit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.reviewId").value(1))
                .andExpect(jsonPath("$.rating").value(5));
    }

    @Test
    void submitReviewRejectsInvalidPayload() throws Exception {
        ReviewRequest request = createRequest();
        request.setAppointmentId(0);
        request.setPatientId(0);
        request.setProviderId(0);
        request.setRating(0);
        request.setComment("x".repeat(2001));

        mockMvc.perform(post("/reviews/submit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation Failed"))
                .andExpect(jsonPath("$.errors.appointmentId").value("Appointment ID must be greater than 0"))
                .andExpect(jsonPath("$.errors.patientId").value("Patient ID must be greater than 0"))
                .andExpect(jsonPath("$.errors.providerId").value("Provider ID must be greater than 0"))
                .andExpect(jsonPath("$.errors.rating").value("Rating must be at least 1"))
                .andExpect(jsonPath("$.errors.comment").value("Comment must be at most 2000 characters"));
    }

    @Test
    void getByProviderReturnsReviews() throws Exception {
        when(reviewService.getReviewsByProvider(202)).thenReturn(List.of(createReview()));

        mockMvc.perform(get("/reviews/provider/202"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].providerId").value(202));
    }

    @Test
    void getByPatientReturnsReviews() throws Exception {
        when(reviewService.getReviewsByPatient(101)).thenReturn(List.of(createReview()));

        mockMvc.perform(get("/reviews/patient/101"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].patientId").value(101));
    }

    @Test
    void getByIdReturnsReview() throws Exception {
        when(reviewService.getReviewById(1)).thenReturn(createReview());

        mockMvc.perform(get("/reviews/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reviewId").value(1));
    }

    @Test
    void updateReviewReturnsUpdatedReview() throws Exception {
        Review updated = createReview();
        updated.setRating(4);
        updated.setComment("Updated comment");

        when(reviewService.updateReview(anyInt(), any(ReviewRequest.class))).thenReturn(updated);

        ReviewRequest request = createRequest();
        request.setRating(4);
        request.setComment("Updated comment");

        mockMvc.perform(put("/reviews/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rating").value(4))
                .andExpect(jsonPath("$.comment").value("Updated comment"));
    }

    @Test
    void deleteReviewReturnsSuccessMessage() throws Exception {
        doNothing().when(reviewService).deleteReview(1);

        mockMvc.perform(delete("/reviews/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Review deleted successfully."));
    }

    @Test
    void getAverageRatingReturnsSummaryPayload() throws Exception {
        when(reviewService.getAverageRating(202)).thenReturn(4.3);

        mockMvc.perform(get("/reviews/provider/202/average"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.providerId").value(202))
                .andExpect(jsonPath("$.averageRating").value(4.3));
    }

    @Test
    void getReviewCountReturnsSummaryPayload() throws Exception {
        when(reviewService.getReviewCount(202)).thenReturn(14L);

        mockMvc.perform(get("/reviews/provider/202/count"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.providerId").value(202))
                .andExpect(jsonPath("$.totalReviews").value(14));
    }

    @Test
    void controllerAdviceHandlesBusinessException() throws Exception {
        when(reviewService.getAverageRating(202))
                .thenThrow(new BadRequestException("Invalid provider id"));

        mockMvc.perform(get("/reviews/provider/202/average"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Invalid provider id"));
    }

    @Test
    void securityConfigurationAllowsPostWithoutCsrfToken() throws Exception {
        when(reviewService.submitReview(any(ReviewRequest.class))).thenReturn(createReview());

        mockMvc.perform(post("/reviews/submit")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest())))
                .andExpect(status().isCreated());
    }

    private ReviewRequest createRequest() {
        ReviewRequest request = new ReviewRequest();
        request.setAppointmentId(500);
        request.setPatientId(101);
        request.setProviderId(202);
        request.setRating(5);
        request.setComment("Excellent consultation");
        request.setAnonymous(true);
        return request;
    }

    private Review createReview() {
        return Review.builder()
                .reviewId(1)
                .appointmentId(500)
                .patientId(101)
                .providerId(202)
                .rating(5)
                .comment("Excellent consultation")
                .isAnonymous(true)
                .createdAt(LocalDateTime.of(2026, 5, 10, 9, 0))
                .updatedAt(LocalDateTime.of(2026, 5, 10, 9, 15))
                .build();
    }
}
