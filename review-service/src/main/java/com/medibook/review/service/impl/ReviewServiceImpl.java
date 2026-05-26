package com.medibook.review.service.impl;

import java.util.List;

import org.springframework.stereotype.Service;

import com.medibook.review.client.AppointmentClient;
import com.medibook.review.client.ProviderClient;
import com.medibook.review.dto.request.AppointmentDto;
import com.medibook.review.dto.request.ReviewRequest;
import com.medibook.review.entity.Review;
import com.medibook.review.exception.BadRequestException;
import com.medibook.review.exception.DuplicateResourceException;
import com.medibook.review.exception.ResourceNotFoundException;
import com.medibook.review.repository.ReviewRepository;
import com.medibook.review.service.ReviewService;
import com.medibook.review.client.NotificationClient;
import com.medibook.review.dto.request.NotificationRequest;

@Service
public class ReviewServiceImpl implements ReviewService {

    private ReviewRepository reviewRepository;
    private AppointmentClient appointmentClient;
    private ProviderClient providerClient;
    private final NotificationClient notificationClient;

    public ReviewServiceImpl(ReviewRepository reviewRepository,
                             AppointmentClient appointmentClient,
                             ProviderClient providerClient, NotificationClient notificationClient) {
        this.reviewRepository  = reviewRepository;
        this.appointmentClient = appointmentClient;
        this.providerClient    = providerClient;
        this.notificationClient = notificationClient;
    }

    @Override
    public Review submitReview(ReviewRequest request) {
        AppointmentDto appointment;
        try {
            appointment = appointmentClient.getById(request.getAppointmentId());
        } catch (Exception e) {
            throw new BadRequestException(
                "Could not fetch appointment #" + request.getAppointmentId()
                + ". Make sure appointment-service is running. Error: " + e.getMessage());
        }

        String status = appointment != null ? appointment.getStatus() : null;
        if (status == null) {
            throw new BadRequestException(
                "Appointment #" + request.getAppointmentId()
                + " returned a null status. Please check appointment-service.");
        }

        if (!status.equalsIgnoreCase("COMPLETED")) {
            throw new BadRequestException(
                "You can only review after appointment is completed. Status: " + status);
        }

        if (reviewRepository.findByAppointmentId(request.getAppointmentId()).isPresent()) {
            throw new DuplicateResourceException(
                "Review already exists for appointment: " + request.getAppointmentId());
        }

        if (request.getRating() < 1 || request.getRating() > 5) {
            throw new BadRequestException("Rating must be between 1 and 5 stars.");
        }

        Review review = Review.builder()
                .appointmentId(request.getAppointmentId())
                .patientId(request.getPatientId())
                .providerId(request.getProviderId())
                .rating(request.getRating())
                .comment(request.getComment())
                .isAnonymous(request.isAnonymous())
                .build();

        Review saved = reviewRepository.save(review);
        updateDoctorRating(request.getProviderId());
        sendNotification(request.getProviderId(), "New Review Received",
            "A patient rated you " + request.getRating() + "★ stars.");
        return saved;
    }

    private void sendNotification(int recipientId, String title, String msg) {
        try {
            NotificationRequest req = new NotificationRequest();
            req.setRecipientId(recipientId);
            req.setType("ANNOUNCEMENT");
            req.setTitle(title);
            req.setMessage(msg);
            req.setChannel("APP");
            notificationClient.send(req);
        } catch (Exception e) {
            System.err.println("[ReviewService] Notification failed: " + e.getMessage());
        }
    }
    

    @Override
    public List<Review> getReviewsByProvider(int providerId) {
        return reviewRepository.findByProviderIdOrderByCreatedAtDesc(providerId);
    }

    @Override
    public List<Review> getReviewsByPatient(int patientId) {
        return reviewRepository.findByPatientId(patientId);
    }

    @Override
    public Review getReviewById(int reviewId) {
        return reviewRepository.findByReviewId(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review", "id", reviewId));
    }

    @Override
    public Review updateReview(int reviewId, ReviewRequest request) {
        Review existing = getReviewById(reviewId);

        if (request.getRating() < 1 || request.getRating() > 5) {
            throw new BadRequestException("Rating must be between 1 and 5 stars.");
        }

        existing.setRating(request.getRating());
        existing.setComment(request.getComment());
        existing.setAnonymous(request.isAnonymous());

        Review saved = reviewRepository.save(existing);
        updateDoctorRating(existing.getProviderId());
        return saved;
    }

    @Override
    public void deleteReview(int reviewId) {
        Review review = getReviewById(reviewId);
        int providerId = review.getProviderId();
        reviewRepository.deleteById(reviewId);
        updateDoctorRating(providerId);
        sendNotification(review.getPatientId(), "Review Removed",
            "Your review has been deleted.");
    }

    @Override
    public double getAverageRating(int providerId) {
        Double avg = reviewRepository.calculateAverageRatingByProviderId(providerId);
        return avg != null ? Math.round(avg * 10.0) / 10.0 : 0.0;
    }

    @Override
    public long getReviewCount(int providerId) {
        return reviewRepository.countByProviderId(providerId);
    }

    private void updateDoctorRating(int providerId) {
        double newAvg = getAverageRating(providerId);
        providerClient.updateRating(providerId, newAvg);
    }
}
