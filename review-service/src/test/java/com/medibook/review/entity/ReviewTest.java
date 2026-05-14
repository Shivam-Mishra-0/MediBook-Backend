package com.medibook.review.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

class ReviewTest {

    @Test
    void builderAndAccessorsExposeExpectedState() {
        Review review = Review.builder()
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

        assertEquals(1, review.getReviewId());
        assertEquals(500, review.getAppointmentId());
        assertEquals(101, review.getPatientId());
        assertEquals(202, review.getProviderId());
        assertEquals(5, review.getRating());
        assertEquals("Excellent consultation", review.getComment());
        org.junit.jupiter.api.Assertions.assertTrue(review.isAnonymous());
        assertEquals(LocalDateTime.of(2026, 5, 10, 9, 0), review.getCreatedAt());
        assertEquals(LocalDateTime.of(2026, 5, 10, 9, 15), review.getUpdatedAt());
    }

    @Test
    void lifecycleCallbacksPopulateAuditFields() {
        Review review = new Review();

        review.prePersist();

        assertNotNull(review.getCreatedAt());
        assertNotNull(review.getUpdatedAt());

        LocalDateTime initialUpdatedAt = review.getUpdatedAt();
        review.preUpdate();

        assertNotNull(review.getUpdatedAt());
        org.junit.jupiter.api.Assertions.assertFalse(review.getUpdatedAt().isBefore(initialUpdatedAt));
    }
}
