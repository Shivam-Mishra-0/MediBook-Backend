package com.medibook.provider.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.medibook.provider.dto.request.ProviderRequest;
import com.medibook.provider.dto.response.ProviderDetailResponse;
import com.medibook.provider.dto.response.UserDto;
import com.medibook.provider.exception.ErrorResponse;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

class ProviderDtoTest {

    private static ValidatorFactory validatorFactory;
    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
        validator = validatorFactory.getValidator();
    }

    @AfterAll
    static void tearDownValidator() {
        validatorFactory.close();
    }

    @Test
    void providerRequestAcceptsValidPayload() {
        ProviderRequest request = createValidRequest();

        Set<ConstraintViolation<ProviderRequest>> violations = validator.validate(request);

        assertTrue(violations.isEmpty());
        assertEquals(77, request.getUserId());
        assertEquals("Cardiology", request.getSpecialization());
        assertEquals("MBBS, MD", request.getQualification());
        assertEquals(8, request.getExperienceYears());
        assertEquals("Experienced cardiologist", request.getBio());
        assertEquals("Heart Care Clinic", request.getClinicName());
        assertEquals("123 Main Street", request.getClinicAddress());
    }

    @Test
    void providerRequestRejectsInvalidPayload() {
        ProviderRequest request = new ProviderRequest();
        request.setUserId(0);
        request.setSpecialization(" ");
        request.setQualification(" ");
        request.setExperienceYears(61);
        request.setBio("x".repeat(1001));
        request.setClinicName(" ");
        request.setClinicAddress(" ");

        Set<String> messages = validator.validate(request)
                .stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.toSet());

        assertTrue(messages.contains("userId must be greater than 0"));
        assertTrue(messages.contains("Specialization is required"));
        assertTrue(messages.contains("Qualification is required"));
        assertTrue(messages.contains("Experience years cannot exceed 60"));
        assertTrue(messages.contains("Bio must be at most 1000 characters"));
        assertTrue(messages.contains("Clinic name is required"));
        assertTrue(messages.contains("Clinic address is required"));
    }

    @Test
    void providerDetailResponseSupportsBuilderAndMutators() {
        ProviderDetailResponse response = ProviderDetailResponse.builder()
                .providerId(10)
                .userId(77)
                .specialization("Cardiology")
                .qualification("MBBS, MD")
                .experienceYears(8)
                .bio("Experienced cardiologist")
                .clinicName("Heart Care Clinic")
                .clinicAddress("123 Main Street")
                .avgRating(4.5)
                .isVerified(true)
                .isAvailable(true)
                .createdAt(LocalDate.of(2026, 5, 10))
                .consultationFee(750.0)
                .fullName("Dr. Ananya Rao")
                .email("doctor@medibook.com")
                .phone("9999999999")
                .profilePicUrl("https://img.test/profile.png")
                .build();

        response.setClinicName("Updated Clinic");

        assertEquals(10, response.getProviderId());
        assertEquals(77, response.getUserId());
        assertEquals("Cardiology", response.getSpecialization());
        assertEquals("MBBS, MD", response.getQualification());
        assertEquals(8, response.getExperienceYears());
        assertEquals("Experienced cardiologist", response.getBio());
        assertEquals("Updated Clinic", response.getClinicName());
        assertEquals("123 Main Street", response.getClinicAddress());
        assertEquals(4.5, response.getAvgRating());
        assertTrue(response.isVerified());
        assertTrue(response.isAvailable());
        assertEquals(LocalDate.of(2026, 5, 10), response.getCreatedAt());
        assertEquals(750.0, response.getConsultationFee());
        assertEquals("Dr. Ananya Rao", response.getFullName());
        assertEquals("doctor@medibook.com", response.getEmail());
        assertEquals("9999999999", response.getPhone());
        assertEquals("https://img.test/profile.png", response.getProfilePicUrl());
    }

    @Test
    void userDtoAndErrorResponseExposeStateThroughAccessors() {
        UserDto userDto = new UserDto();
        userDto.setUserId(77);
        userDto.setFullName("Dr. Ananya Rao");
        userDto.setEmail("doctor@medibook.com");
        userDto.setPhone("9999999999");
        userDto.setProfilePicUrl("https://img.test/profile.png");
        userDto.setRole("PROVIDER");

        ErrorResponse errorResponse = new ErrorResponse();
        errorResponse.setStatus(400);
        errorResponse.setError("Bad Request");
        errorResponse.setMessage("Validation failed");
        errorResponse.setPath("/providers/register");
        errorResponse.setTimestamp(java.time.LocalDateTime.of(2026, 5, 10, 9, 0));

        assertEquals(77, userDto.getUserId());
        assertEquals("Dr. Ananya Rao", userDto.getFullName());
        assertEquals("doctor@medibook.com", userDto.getEmail());
        assertEquals("9999999999", userDto.getPhone());
        assertEquals("https://img.test/profile.png", userDto.getProfilePicUrl());
        assertEquals("PROVIDER", userDto.getRole());

        assertEquals(400, errorResponse.getStatus());
        assertEquals("Bad Request", errorResponse.getError());
        assertEquals("Validation failed", errorResponse.getMessage());
        assertEquals("/providers/register", errorResponse.getPath());
        assertEquals(java.time.LocalDateTime.of(2026, 5, 10, 9, 0), errorResponse.getTimestamp());
    }

    private ProviderRequest createValidRequest() {
        ProviderRequest request = new ProviderRequest();
        request.setUserId(77);
        request.setSpecialization("Cardiology");
        request.setQualification("MBBS, MD");
        request.setExperienceYears(8);
        request.setBio("Experienced cardiologist");
        request.setClinicName("Heart Care Clinic");
        request.setClinicAddress("123 Main Street");
        return request;
    }
}
