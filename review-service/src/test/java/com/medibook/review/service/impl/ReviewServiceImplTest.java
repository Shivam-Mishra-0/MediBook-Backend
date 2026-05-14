package com.medibook.review.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.medibook.review.client.AppointmentClient;
import com.medibook.review.client.ProviderClient;
import com.medibook.review.dto.request.AppointmentDto;
import com.medibook.review.dto.request.ReviewRequest;
import com.medibook.review.entity.Review;
import com.medibook.review.exception.BadRequestException;
import com.medibook.review.exception.DuplicateResourceException;
import com.medibook.review.exception.ResourceNotFoundException;
import com.medibook.review.repository.ReviewRepository;

@ExtendWith(MockitoExtension.class)
class ReviewServiceImplTest {

    @Mock
    private ReviewRepository reviewRepository;

    @Mock
    private AppointmentClient appointmentClient;

    @Mock
    private ProviderClient providerClient;

    @InjectMocks
    private ReviewServiceImpl reviewService;

    @Test
    void submitReviewSavesReviewAndUpdatesProviderRating() {
        ReviewRequest request = createRequest();
        Review savedReview = createReview();

        when(appointmentClient.getById(500)).thenReturn(createAppointment("COMPLETED"));
        when(reviewRepository.findByAppointmentId(500)).thenReturn(Optional.empty());
        when(reviewRepository.save(any(Review.class))).thenReturn(savedReview);
        when(reviewRepository.calculateAverageRatingByProviderId(202)).thenReturn(4.25);

        Review result = reviewService.submitReview(request);

        ArgumentCaptor<Review> captor = ArgumentCaptor.forClass(Review.class);
        verify(reviewRepository).save(captor.capture());
        Review reviewToSave = captor.getValue();

        assertSame(savedReview, result);
        assertEquals(500, reviewToSave.getAppointmentId());
        assertEquals(101, reviewToSave.getPatientId());
        assertEquals(202, reviewToSave.getProviderId());
        assertEquals(5, reviewToSave.getRating());
        assertEquals("Excellent consultation", reviewToSave.getComment());
        org.junit.jupiter.api.Assertions.assertTrue(reviewToSave.isAnonymous());
        verify(providerClient).updateRating(202, 4.3);
    }

    @Test
    void submitReviewRejectsWhenAppointmentLookupFails() {
        ReviewRequest request = createRequest();
        when(appointmentClient.getById(500)).thenThrow(new RuntimeException("service unavailable"));

        BadRequestException exception = assertThrows(
                BadRequestException.class,
                () -> reviewService.submitReview(request));

        assertEquals(
                "Could not fetch appointment #500. Make sure appointment-service is running. Error: service unavailable",
                exception.getMessage());
        verify(reviewRepository, never()).save(any(Review.class));
        verify(providerClient, never()).updateRating(anyInt(), anyDouble());
    }

    @Test
    void submitReviewRejectsNullAppointmentStatus() {
        ReviewRequest request = createRequest();
        when(appointmentClient.getById(500)).thenReturn(createAppointment(null));

        BadRequestException exception = assertThrows(
                BadRequestException.class,
                () -> reviewService.submitReview(request));

        assertEquals(
                "Appointment #500 returned a null status. Please check appointment-service.",
                exception.getMessage());
    }

    @Test
    void submitReviewRejectsWhenAppointmentNotCompleted() {
        ReviewRequest request = createRequest();
        when(appointmentClient.getById(500)).thenReturn(createAppointment("SCHEDULED"));

        BadRequestException exception = assertThrows(
                BadRequestException.class,
                () -> reviewService.submitReview(request));

        assertEquals(
                "You can only review after appointment is completed. Status: SCHEDULED",
                exception.getMessage());
    }

    @Test
    void submitReviewRejectsDuplicateReview() {
        ReviewRequest request = createRequest();
        when(appointmentClient.getById(500)).thenReturn(createAppointment("COMPLETED"));
        when(reviewRepository.findByAppointmentId(500)).thenReturn(Optional.of(createReview()));

        DuplicateResourceException exception = assertThrows(
                DuplicateResourceException.class,
                () -> reviewService.submitReview(request));

        assertEquals("Review already exists for appointment: 500", exception.getMessage());
    }

    @Test
    void submitReviewRejectsRatingsOutsideAllowedRange() {
        ReviewRequest lowRequest = createRequest();
        lowRequest.setRating(0);
        ReviewRequest highRequest = createRequest();
        highRequest.setRating(6);

        when(appointmentClient.getById(500)).thenReturn(createAppointment("COMPLETED"));
        when(reviewRepository.findByAppointmentId(500)).thenReturn(Optional.empty());

        BadRequestException low = assertThrows(
                BadRequestException.class,
                () -> reviewService.submitReview(lowRequest));
        BadRequestException high = assertThrows(
                BadRequestException.class,
                () -> reviewService.submitReview(highRequest));

        assertEquals("Rating must be between 1 and 5 stars.", low.getMessage());
        assertEquals("Rating must be between 1 and 5 stars.", high.getMessage());
    }

    @Test
    void getReviewsByProviderReturnsRepositoryResults() {
        List<Review> reviews = List.of(createReview());
        when(reviewRepository.findByProviderIdOrderByCreatedAtDesc(202)).thenReturn(reviews);

        List<Review> result = reviewService.getReviewsByProvider(202);

        assertSame(reviews, result);
    }

    @Test
    void getReviewsByPatientReturnsRepositoryResults() {
        List<Review> reviews = List.of(createReview());
        when(reviewRepository.findByPatientId(101)).thenReturn(reviews);

        List<Review> result = reviewService.getReviewsByPatient(101);

        assertSame(reviews, result);
    }

    @Test
    void getReviewByIdReturnsReview() {
        Review review = createReview();
        when(reviewRepository.findByReviewId(1)).thenReturn(Optional.of(review));

        Review result = reviewService.getReviewById(1);

        assertSame(review, result);
    }

    @Test
    void getReviewByIdThrowsWhenMissing() {
        when(reviewRepository.findByReviewId(1)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> reviewService.getReviewById(1));

        assertEquals("Review not found with id: 1", exception.getMessage());
    }

    @Test
    void updateReviewUpdatesFieldsAndProviderRating() {
        Review existing = createReview();
        ReviewRequest request = createRequest();
        request.setRating(4);
        request.setComment("Updated comment");
        request.setAnonymous(false);

        when(reviewRepository.findByReviewId(1)).thenReturn(Optional.of(existing));
        when(reviewRepository.save(existing)).thenReturn(existing);
        when(reviewRepository.calculateAverageRatingByProviderId(202)).thenReturn(3.94);

        Review result = reviewService.updateReview(1, request);

        assertSame(existing, result);
        assertEquals(4, existing.getRating());
        assertEquals("Updated comment", existing.getComment());
        org.junit.jupiter.api.Assertions.assertFalse(existing.isAnonymous());
        verify(providerClient).updateRating(202, 3.9);
    }

    @Test
    void updateReviewRejectsInvalidRating() {
        ReviewRequest request = createRequest();
        request.setRating(6);
        when(reviewRepository.findByReviewId(1)).thenReturn(Optional.of(createReview()));

        BadRequestException exception = assertThrows(
                BadRequestException.class,
                () -> reviewService.updateReview(1, request));

        assertEquals("Rating must be between 1 and 5 stars.", exception.getMessage());
        verify(reviewRepository, never()).save(any(Review.class));
    }

    @Test
    void deleteReviewDeletesExistingReviewAndUpdatesProviderRating() {
        Review review = createReview();
        when(reviewRepository.findByReviewId(1)).thenReturn(Optional.of(review));
        when(reviewRepository.calculateAverageRatingByProviderId(202)).thenReturn(4.0);

        reviewService.deleteReview(1);

        verify(reviewRepository).deleteById(1);
        verify(providerClient).updateRating(202, 4.0);
    }

    @Test
    void getAverageRatingRoundsToOneDecimal() {
        when(reviewRepository.calculateAverageRatingByProviderId(202)).thenReturn(4.26);

        double result = reviewService.getAverageRating(202);

        assertEquals(4.3, result);
    }

    @Test
    void getAverageRatingReturnsZeroWhenRepositoryReturnsNull() {
        when(reviewRepository.calculateAverageRatingByProviderId(202)).thenReturn(null);

        double result = reviewService.getAverageRating(202);

        assertEquals(0.0, result);
    }

    @Test
    void getReviewCountReturnsRepositoryCount() {
        when(reviewRepository.countByProviderId(202)).thenReturn(12L);

        long result = reviewService.getReviewCount(202);

        assertEquals(12L, result);
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

    private AppointmentDto createAppointment(String status) {
        AppointmentDto appointment = new AppointmentDto();
        appointment.setAppointmentId(500);
        appointment.setPatientId(101);
        appointment.setProviderId(202);
        appointment.setStatus(status);
        return appointment;
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
