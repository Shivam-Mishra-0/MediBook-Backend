package com.medibook.provider.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.medibook.provider.dto.request.ProviderRequest;
import com.medibook.provider.entity.Provider;
import com.medibook.provider.exception.BadRequestException;
import com.medibook.provider.exception.DuplicateResourceException;
import com.medibook.provider.exception.ResourceNotFoundException;
import com.medibook.provider.repository.ProviderRepository;

@ExtendWith(MockitoExtension.class)
class ProviderServiceImplTest {

    @Mock
    private ProviderRepository providerRepository;

    @InjectMocks
    private ProviderServiceImpl providerService;

    @Test
    void registerProviderSavesProviderWithExpectedDefaults() {
        ProviderRequest request = createRequest();
        Provider savedProvider = createProvider();
        savedProvider.setProviderId(99);

        when(providerRepository.findByUserId(request.getUserId())).thenReturn(Optional.empty());
        when(providerRepository.save(any(Provider.class))).thenReturn(savedProvider);

        Provider result = providerService.registerProvider(request);

        ArgumentCaptor<Provider> providerCaptor = ArgumentCaptor.forClass(Provider.class);
        verify(providerRepository).save(providerCaptor.capture());
        Provider providerToSave = providerCaptor.getValue();

        assertSame(savedProvider, result);
        assertEquals(request.getUserId(), providerToSave.getUserId());
        assertEquals(request.getSpecialization(), providerToSave.getSpecialization());
        assertEquals(request.getQualification(), providerToSave.getQualification());
        assertEquals(request.getExperienceYears(), providerToSave.getExperienceYears());
        assertEquals(request.getBio(), providerToSave.getBio());
        assertEquals(request.getClinicName(), providerToSave.getClinicName());
        assertEquals(request.getClinicAddress(), providerToSave.getClinicAddress());
        assertEquals(0.0, providerToSave.getAvgRating());
        assertFalse(providerToSave.isVerified());
        assertTrue(providerToSave.isAvailable());
    }

    @Test
    void registerProviderThrowsWhenProfileAlreadyExists() {
        ProviderRequest request = createRequest();
        when(providerRepository.findByUserId(request.getUserId())).thenReturn(Optional.of(createProvider()));

        DuplicateResourceException exception = assertThrows(
                DuplicateResourceException.class,
                () -> providerService.registerProvider(request));

        assertEquals("Provider profile already exists with userId: 77", exception.getMessage());
        verify(providerRepository, never()).save(any(Provider.class));
    }

    @Test
    void getProviderByIdReturnsProvider() {
        Provider provider = createProvider();
        when(providerRepository.findById(10)).thenReturn(Optional.of(provider));

        Provider result = providerService.getProviderById(10);

        assertSame(provider, result);
    }

    @Test
    void getProviderByIdThrowsWhenMissing() {
        when(providerRepository.findById(10)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> providerService.getProviderById(10));

        assertEquals("Provider not found with id: 10", exception.getMessage());
    }

    @Test
    void getProviderByUserIdReturnsProvider() {
        Provider provider = createProvider();
        when(providerRepository.findByUserId(77)).thenReturn(Optional.of(provider));

        Provider result = providerService.getProviderByUserId(77);

        assertSame(provider, result);
    }

    @Test
    void getProviderByUserIdThrowsWhenMissing() {
        when(providerRepository.findByUserId(77)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> providerService.getProviderByUserId(77));

        assertEquals("Provider not found with userId: 77", exception.getMessage());
    }

    @Test
    void getBySpecializationRejectsBlankValues() {
        BadRequestException exception = assertThrows(
                BadRequestException.class,
                () -> providerService.getBySpecialization("  "));

        assertEquals("Specialization cannot be empty.", exception.getMessage());
    }

    @Test
    void getBySpecializationReturnsOnlyVerifiedProviders() {
        Provider verified = createProvider();
        Provider unverified = createProvider();
        unverified.setVerified(false);

        when(providerRepository.findBySpecialization("Cardiology"))
                .thenReturn(List.of(verified, unverified));

        List<Provider> result = providerService.getBySpecialization("Cardiology");

        assertEquals(1, result.size());
        assertSame(verified, result.get(0));
    }

    @Test
    void searchProvidersRejectsBlankKeyword() {
        BadRequestException exception = assertThrows(
                BadRequestException.class,
                () -> providerService.searchProviders(" "));

        assertEquals("Search keyword cannot be empty.", exception.getMessage());
    }

    @Test
    void searchProvidersDelegatesToRepository() {
        List<Provider> expected = List.of(createProvider());
        when(providerRepository.searchByNameOrSpecialization("heart")).thenReturn(expected);

        List<Provider> result = providerService.searchProviders("heart");

        assertSame(expected, result);
    }

    @Test
    void updateProviderUpdatesMutableFields() {
        Provider existing = createProvider();
        ProviderRequest request = createRequest();
        request.setSpecialization("Neurology");
        request.setQualification("MD Neurology");
        request.setExperienceYears(12);
        request.setBio("Updated bio");
        request.setClinicName("City Neuro Center");
        request.setClinicAddress("Downtown");

        when(providerRepository.findById(10)).thenReturn(Optional.of(existing));
        when(providerRepository.save(existing)).thenReturn(existing);

        Provider result = providerService.updateProvider(10, request);

        assertSame(existing, result);
        assertEquals("Neurology", existing.getSpecialization());
        assertEquals("MD Neurology", existing.getQualification());
        assertEquals(12, existing.getExperienceYears());
        assertEquals("Updated bio", existing.getBio());
        assertEquals("City Neuro Center", existing.getClinicName());
        assertEquals("Downtown", existing.getClinicAddress());
    }

    @Test
    void updateProviderRejectsNegativeExperience() {
        ProviderRequest request = createRequest();
        request.setExperienceYears(-1);

        when(providerRepository.findById(10)).thenReturn(Optional.of(createProvider()));

        BadRequestException exception = assertThrows(
                BadRequestException.class,
                () -> providerService.updateProvider(10, request));

        assertEquals("Experience years cannot be negative.", exception.getMessage());
        verify(providerRepository, never()).save(any(Provider.class));
    }

    @Test
    void verifyProviderMarksProviderAsVerified() {
        Provider provider = createProvider();
        provider.setVerified(false);

        when(providerRepository.findById(10)).thenReturn(Optional.of(provider));
        when(providerRepository.save(provider)).thenReturn(provider);

        Provider result = providerService.verifyProvider(10);

        assertSame(provider, result);
        assertTrue(provider.isVerified());
    }

    @Test
    void verifyProviderThrowsWhenMissing() {
        when(providerRepository.findById(10)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> providerService.verifyProvider(10));

        assertEquals("Provider not found with id: 10", exception.getMessage());
    }

    @Test
    void setAvailabilityUpdatesProviderFlag() {
        Provider provider = createProvider();
        when(providerRepository.findById(10)).thenReturn(Optional.of(provider));

        providerService.setAvailability(10, false);

        assertFalse(provider.isAvailable());
        verify(providerRepository).save(provider);
    }

    @Test
    void deleteProviderRemovesExistingProvider() {
        when(providerRepository.findById(10)).thenReturn(Optional.of(createProvider()));

        providerService.deleteProvider(10);

        verify(providerRepository).deleteById(10);
    }

    @Test
    void updateRatingRejectsValuesOutsideAllowedRange() {
        BadRequestException lowException = assertThrows(
                BadRequestException.class,
                () -> providerService.updateRating(10, -0.1));
        BadRequestException highException = assertThrows(
                BadRequestException.class,
                () -> providerService.updateRating(10, 5.1));

        assertEquals("Rating must be between 0.0 and 5.0.", lowException.getMessage());
        assertEquals("Rating must be between 0.0 and 5.0.", highException.getMessage());
    }

    @Test
    void updateRatingPersistsNewAverage() {
        Provider provider = createProvider();
        when(providerRepository.findById(10)).thenReturn(Optional.of(provider));

        providerService.updateRating(10, 4.7);

        assertEquals(4.7, provider.getAvgRating());
        verify(providerRepository).save(provider);
    }

    @Test
    void getAllProvidersReturnsEveryProvider() {
        List<Provider> providers = List.of(createProvider());
        when(providerRepository.findAll()).thenReturn(providers);

        List<Provider> result = providerService.getAllProviders();

        assertSame(providers, result);
    }

    @Test
    void getVerifiedAndAvailableProvidersDelegatesToRepository() {
        List<Provider> providers = List.of(createProvider());
        when(providerRepository.findByVerifiedAndIsAvailable(true, true)).thenReturn(providers);

        List<Provider> result = providerService.getVerifiedAndAvailableProviders();

        assertSame(providers, result);
    }

    private ProviderRequest createRequest() {
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

    private Provider createProvider() {
        return Provider.builder()
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
                .verified(true)
                .isAvailable(true)
                .createdAt(LocalDate.of(2026, 5, 10))
                .build();
    }
}
