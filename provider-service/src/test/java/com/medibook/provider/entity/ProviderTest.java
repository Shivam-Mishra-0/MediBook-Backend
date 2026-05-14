package com.medibook.provider.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;

class ProviderTest {

    @Test
    void builderAndAccessorsExposeExpectedState() {
        Provider provider = Provider.builder()
                .providerId(10)
                .userId(77)
                .specialization("Cardiology")
                .qualification("MBBS, MD")
                .experienceYears(8)
                .bio("Experienced cardiologist")
                .clinicName("Heart Care Clinic")
                .clinicAddress("123 Main Street")
                .avgRating(4.5)
                .consultationFee(750.0)
                .verified(false)
                .createdAt(LocalDate.of(2026, 5, 10))
                .build();

        assertEquals(10, provider.getProviderId());
        assertEquals(77, provider.getUserId());
        assertEquals("Cardiology", provider.getSpecialization());
        assertEquals("MBBS, MD", provider.getQualification());
        assertEquals(8, provider.getExperienceYears());
        assertEquals("Experienced cardiologist", provider.getBio());
        assertEquals("Heart Care Clinic", provider.getClinicName());
        assertEquals("123 Main Street", provider.getClinicAddress());
        assertEquals(4.5, provider.getAvgRating());
        assertEquals(750.0, provider.getConsultationFee());
        assertFalse(provider.isVerified());
        assertTrue(provider.isAvailable());
        assertEquals(LocalDate.of(2026, 5, 10), provider.getCreatedAt());

        provider.setVerified(true);
        provider.setAvailable(false);
        provider.setAvgRating(4.9);

        assertTrue(provider.isVerified());
        assertFalse(provider.isAvailable());
        assertEquals(4.9, provider.getAvgRating());
    }

    @Test
    void prePersistSetsCreatedAt() {
        Provider provider = new Provider();

        provider.prePersist();

        assertNotNull(provider.getCreatedAt());
        assertEquals(LocalDate.now(), provider.getCreatedAt());
    }
}
