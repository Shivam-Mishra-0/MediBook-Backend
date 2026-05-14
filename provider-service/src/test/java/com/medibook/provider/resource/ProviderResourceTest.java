package com.medibook.provider.resource;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
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
import com.medibook.provider.client.UserClient;
import com.medibook.provider.config.SecurityConfig;
import com.medibook.provider.dto.request.ProviderRequest;
import com.medibook.provider.dto.response.UserDto;
import com.medibook.provider.entity.Provider;
import com.medibook.provider.exception.BadRequestException;
import com.medibook.provider.exception.GlobalExceptionHandler;
import com.medibook.provider.service.ProviderService;

@WebMvcTest(ProviderResource.class)
@AutoConfigureMockMvc(addFilters = true)
@Import({ SecurityConfig.class, GlobalExceptionHandler.class })
class ProviderResourceTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ProviderService providerService;

    @MockBean
    private UserClient userClient;

    @Test
    void registerProviderReturnsCreatedPayload() throws Exception {
        Provider provider = createProvider();
        provider.setVerified(false);

        when(providerService.registerProvider(any(ProviderRequest.class))).thenReturn(provider);

        mockMvc.perform(post("/providers/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value(
                        "Provider profile created successfully. Waiting for admin verification."))
                .andExpect(jsonPath("$.providerId").value(10))
                .andExpect(jsonPath("$.isVerified").value(false));
    }

    @Test
    void registerProviderRejectsInvalidPayload() throws Exception {
        ProviderRequest request = createRequest();
        request.setUserId(0);
        request.setClinicName(" ");
        request.setClinicAddress(" ");

        mockMvc.perform(post("/providers/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation Failed"))
                .andExpect(jsonPath("$.errors.userId").value("userId must be greater than 0"))
                .andExpect(jsonPath("$.errors.clinicName").value("Clinic name is required"))
                .andExpect(jsonPath("$.errors.clinicAddress").value("Clinic address is required"));
    }

    @Test
    void getByIdReturnsProviderDetailsWithUserData() throws Exception {
        Provider provider = createProvider();
        UserDto user = createUser();

        when(providerService.getProviderById(10)).thenReturn(provider);
        when(userClient.getUserById(77)).thenReturn(user);

        mockMvc.perform(get("/providers/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.providerId").value(10))
                .andExpect(jsonPath("$.fullName").value("Dr. Ananya Rao"))
                .andExpect(jsonPath("$.email").value("doctor@medibook.com"))
                .andExpect(jsonPath("$.phone").value("9999999999"))
                .andExpect(jsonPath("$.profilePicUrl").value("https://img.test/profile.png"))
                .andExpect(jsonPath("$.consultationFee").value(750.0))
                .andExpect(jsonPath("$.isVerified").value(true))
                .andExpect(jsonPath("$.isAvailable").value(true));
    }

    @Test
    void getByIdFallsBackWhenUserServiceFails() throws Exception {
        Provider provider = createProvider();

        when(providerService.getProviderById(10)).thenReturn(provider);
        when(userClient.getUserById(77)).thenThrow(new RuntimeException("auth-service unavailable"));

        mockMvc.perform(get("/providers/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value("Provider #10"))
                .andExpect(jsonPath("$.email").value(""))
                .andExpect(jsonPath("$.phone").value(""))
                .andExpect(jsonPath("$.profilePicUrl").value(""));
    }

    @Test
    void getByUserIdReturnsProvider() throws Exception {
        when(providerService.getProviderByUserId(77)).thenReturn(createProvider());

        mockMvc.perform(get("/providers/user/77"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.providerId").value(10))
                .andExpect(jsonPath("$.userId").value(77));
    }

    @Test
    void getBySpecializationReturnsProviders() throws Exception {
        when(providerService.getBySpecialization("Cardiology")).thenReturn(List.of(createProvider()));

        mockMvc.perform(get("/providers/specialization/Cardiology"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].specialization").value("Cardiology"));
    }

    @Test
    void searchReturnsProviders() throws Exception {
        when(providerService.searchProviders("heart")).thenReturn(List.of(createProvider()));

        mockMvc.perform(get("/providers/search").param("keyword", "heart"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].providerId").value(10));
    }

    @Test
    void getAvailableReturnsProviders() throws Exception {
        when(providerService.getVerifiedAndAvailableProviders()).thenReturn(List.of(createProvider()));

        mockMvc.perform(get("/providers/available"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].available").value(true));
    }

    @Test
    void getAllBuildsDetailResponsesForEveryProvider() throws Exception {
        Provider firstProvider = createProvider();
        Provider secondProvider = createProvider();
        secondProvider.setProviderId(11);
        secondProvider.setUserId(78);
        secondProvider.setClinicName("Second Clinic");

        UserDto firstUser = createUser();
        UserDto secondUser = new UserDto();
        secondUser.setUserId(78);
        secondUser.setFullName("Dr. Kiran Sen");
        secondUser.setEmail("kiran@medibook.com");
        secondUser.setPhone("8888888888");
        secondUser.setProfilePicUrl("https://img.test/kiran.png");

        when(providerService.getAllProviders()).thenReturn(List.of(firstProvider, secondProvider));
        when(userClient.getUserById(77)).thenReturn(firstUser);
        when(userClient.getUserById(78)).thenReturn(secondUser);

        mockMvc.perform(get("/providers/all"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].fullName").value("Dr. Ananya Rao"))
                .andExpect(jsonPath("$[1].providerId").value(11))
                .andExpect(jsonPath("$[1].clinicName").value("Second Clinic"))
                .andExpect(jsonPath("$[1].fullName").value("Dr. Kiran Sen"));
    }

    @Test
    void updateProviderReturnsUpdatedProvider() throws Exception {
        Provider updatedProvider = createProvider();
        updatedProvider.setSpecialization("Neurology");

        when(providerService.updateProvider(eq(10), any(ProviderRequest.class))).thenReturn(updatedProvider);

        ProviderRequest request = createRequest();
        request.setSpecialization("Neurology");

        mockMvc.perform(put("/providers/10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.specialization").value("Neurology"));
    }

    @Test
    void verifyProviderReturnsUpdatedProvider() throws Exception {
        Provider provider = createProvider();
        provider.setVerified(true);

        when(providerService.verifyProvider(10)).thenReturn(provider);

        mockMvc.perform(put("/providers/10/verify"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isVerified").value(true));
    }

    @Test
    void setAvailabilityReturnsFriendlyMessage() throws Exception {
        doNothing().when(providerService).setAvailability(10, false);

        mockMvc.perform(put("/providers/10/availability").param("isAvailable", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Doctor is now unavailable for appointments."));
    }

    @Test
    void deleteProviderReturnsSuccessMessage() throws Exception {
        doNothing().when(providerService).deleteProvider(10);

        mockMvc.perform(delete("/providers/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Provider deleted successfully."));
    }

    @Test
    void updateRatingDelegatesToService() throws Exception {
        doNothing().when(providerService).updateRating(10, 4.8);

        mockMvc.perform(put("/providers/10/rating").param("avgRating", "4.8"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Rating updated."));
    }

    @Test
    void controllerAdviceHandlesBusinessExceptions() throws Exception {
        when(providerService.searchProviders("invalid"))
                .thenThrow(new BadRequestException("Search keyword cannot be empty."));

        mockMvc.perform(get("/providers/search").param("keyword", "invalid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Search keyword cannot be empty."));
    }

    @Test
    void securityConfigurationAllowsUnsafeMethodsWithoutCsrfToken() throws Exception {
        Provider provider = createProvider();
        when(providerService.registerProvider(any(ProviderRequest.class))).thenReturn(provider);

        mockMvc.perform(post("/providers/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createRequest())))
                .andExpect(status().isCreated());
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

    private UserDto createUser() {
        UserDto user = new UserDto();
        user.setUserId(77);
        user.setFullName("Dr. Ananya Rao");
        user.setEmail("doctor@medibook.com");
        user.setPhone("9999999999");
        user.setProfilePicUrl("https://img.test/profile.png");
        user.setRole("PROVIDER");
        return user;
    }
}
