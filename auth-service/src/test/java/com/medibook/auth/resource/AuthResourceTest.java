package com.medibook.auth.resource;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.medibook.auth.dto.request.AddPhoneRequest;
import com.medibook.auth.dto.request.ChangePasswordRequest;
import com.medibook.auth.dto.request.EmailRequest;
import com.medibook.auth.dto.request.GoogleLoginCompletionRequest;
import com.medibook.auth.dto.request.LoginRequest;
import com.medibook.auth.dto.request.OtpVerificationRequest;
import com.medibook.auth.dto.request.RegisterAdminRequest;
import com.medibook.auth.dto.request.RegisterRequest;
import com.medibook.auth.dto.request.ResetPasswordRequest;
import com.medibook.auth.dto.request.ResetPasswordVerificationRequest;
import com.medibook.auth.entity.User;
import com.medibook.auth.exception.GlobalExceptionHandler;
import com.medibook.auth.security.JwtUtil;
import com.medibook.auth.service.AuthService;

@ExtendWith(MockitoExtension.class)
class AuthResourceTest {

    @Mock
    private AuthService authService;

    @Mock
    private JwtUtil jwtUtil;

    @InjectMocks
    private AuthResource authResource;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        LocalValidatorFactoryBean validator = new LocalValidatorFactoryBean();
        validator.afterPropertiesSet();

        objectMapper = Jackson2ObjectMapperBuilder.json().build();
        ReflectionTestUtils.setField(authResource, "adminSecretCode", "SECRET");

        mockMvc = MockMvcBuilders.standaloneSetup(authResource)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setValidator(validator)
                .build();
    }

    @Test
    @DisplayName("register returns 201 with user summary")
    void registerSuccess() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setFullName("Riya Sharma");
        request.setEmail("riya@test.com");
        request.setPassword("Password@1");
        request.setRole("Patient");

        when(authService.register(any(RegisterRequest.class))).thenReturn(user(1, "riya@test.com", "Patient", true, true));

        mockMvc.perform(post("/auth/register")
                        .contentType(APPLICATION_JSON)
                        .content(writeJson(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("Registration successful"))
                .andExpect(jsonPath("$.userId").value(1))
                .andExpect(jsonPath("$.role").value("Patient"));
    }

    @Test
    @DisplayName("register returns 400 when request body is invalid")
    void registerValidationFailure() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setFullName("");
        request.setEmail("bad-email");
        request.setPassword("123");
        request.setRole("Admin");

        mockMvc.perform(post("/auth/register")
                        .contentType(APPLICATION_JSON)
                        .content(writeJson(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Validation Failed"))
                .andExpect(jsonPath("$.errors.fullName").value("Full name is required"))
                .andExpect(jsonPath("$.errors.email").value("Valid email is required"))
                .andExpect(jsonPath("$.errors.password").value("Password must be at least 6 characters"))
                .andExpect(jsonPath("$.errors.role").value("Role must be Patient or Provider"));
    }

    @Test
    @DisplayName("login blocks users who still need to add a phone number")
    void loginRequiresPhone() throws Exception {
        LoginRequest request = loginRequest();
        User user = user(1, "riya@test.com", "Patient", true, false);
        user.setPhone(" ");
        user.setProvider(null);

        when(authService.getUserByEmail("riya@test.com")).thenReturn(user);

        mockMvc.perform(post("/auth/login")
                        .contentType(APPLICATION_JSON)
                        .content(writeJson(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Phone number required"))
                .andExpect(jsonPath("$.requiresPhone").value(true))
                .andExpect(jsonPath("$.email").value("riya@test.com"));

        verify(authService).login(any(LoginRequest.class));
    }

    @Test
    @DisplayName("login returns a JWT for verified users")
    void loginVerifiedUserReturnsToken() throws Exception {
        LoginRequest request = loginRequest();
        User user = user(3, "riya@test.com", "Patient", true, true);
        user.setPhone("9876543210");
        when(authService.getUserByEmail("riya@test.com")).thenReturn(user);
        when(jwtUtil.generateToken("riya@test.com", "Patient", 3)).thenReturn("jwt-token");

        mockMvc.perform(post("/auth/login")
                        .contentType(APPLICATION_JSON)
                        .content(writeJson(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt-token"))
                .andExpect(jsonPath("$.role").value("Patient"))
                .andExpect(jsonPath("$.email").value("riya@test.com"))
                .andExpect(jsonPath("$.message").value("Login successful"));
    }

    @Test
    @DisplayName("login sends OTP for first-time users")
    void loginSendsOtpForUnverifiedUser() throws Exception {
        LoginRequest request = loginRequest();
        User user = user(4, "riya@test.com", "Patient", true, false);
        user.setPhone("9876543210");

        when(authService.getUserByEmail("riya@test.com")).thenReturn(user);

        mockMvc.perform(post("/auth/login")
                        .contentType(APPLICATION_JSON)
                        .content(writeJson(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.otpSent").value(true))
                .andExpect(jsonPath("$.email").value("riya@test.com"));

        verify(authService).sendOtp("riya@test.com");
    }

    @Test
    @DisplayName("verify-otp returns a token and persists first verification")
    void verifyOtpSuccess() throws Exception {
        OtpVerificationRequest request = new OtpVerificationRequest();
        request.setEmail("riya@test.com");
        request.setOtp("123456");

        User user = user(5, "riya@test.com", "Provider", true, false);
        user.setPhone("9876543210");
        when(authService.getUserByEmail("riya@test.com")).thenReturn(user);
        when(jwtUtil.generateToken("riya@test.com", "Provider", 5)).thenReturn("otp-token");

        mockMvc.perform(post("/auth/verify-otp")
                        .contentType(APPLICATION_JSON)
                        .content(writeJson(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("otp-token"))
                .andExpect(jsonPath("$.userId").value(5))
                .andExpect(jsonPath("$.role").value("Provider"));

        verify(authService).verifyOtp("riya@test.com", "123456");
        verify(authService).updateProfile(eq(5), any(User.class));
    }

    @Test
    @DisplayName("verify-otp rejects malformed OTP bodies")
    void verifyOtpValidationFailure() throws Exception {
        OtpVerificationRequest request = new OtpVerificationRequest();
        request.setEmail("riya@test.com");
        request.setOtp("123");

        mockMvc.perform(post("/auth/verify-otp")
                        .contentType(APPLICATION_JSON)
                        .content(writeJson(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.otp").value("OTP must be a 6-digit number"));
    }

    @Test
    @DisplayName("resend-otp validates email and delegates to the service")
    void resendOtpSuccess() throws Exception {
        EmailRequest request = new EmailRequest();
        request.setEmail("riya@test.com");

        mockMvc.perform(post("/auth/resend-otp")
                        .contentType(APPLICATION_JSON)
                        .content(writeJson(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.otpSent").value(true));

        verify(authService).sendOtp("riya@test.com");
    }

    @Test
    @DisplayName("logout rejects invalid authorization headers")
    void logoutRejectsInvalidHeader() throws Exception {
        mockMvc.perform(post("/auth/logout")
                        .header("Authorization", "Basic invalid"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Authorization header must contain a Bearer token."));
    }

    @Test
    @DisplayName("logout extracts the bearer token")
    void logoutSuccess() throws Exception {
        mockMvc.perform(post("/auth/logout")
                        .header("Authorization", "Bearer access-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Logged out successfully"));

        verify(authService).logout("access-token");
    }

    @Test
    @DisplayName("refresh returns a new token")
    void refreshSuccess() throws Exception {
        when(authService.refreshToken("refresh-token")).thenReturn("new-token");

        mockMvc.perform(post("/auth/refresh")
                        .header("Authorization", "Bearer refresh-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("new-token"));
    }

    @Test
    @DisplayName("get profile returns the user object")
    void getProfileSuccess() throws Exception {
        when(authService.getUserById(11)).thenReturn(user(11, "profile@test.com", "Patient", true, true));

        mockMvc.perform(get("/auth/profile/11"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(11))
                .andExpect(jsonPath("$.email").value("profile@test.com"));
    }

    @Test
    @DisplayName("update profile returns the saved user")
    void updateProfileSuccess() throws Exception {
        User request = user(12, "update@test.com", "Patient", true, true);
        request.setFullName("Updated Name");
        when(authService.updateProfile(eq(12), any(User.class))).thenReturn(request);

        mockMvc.perform(put("/auth/profile/12")
                        .contentType(APPLICATION_JSON)
                        .content(writeJson(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value("Updated Name"));
    }

    @Test
    @DisplayName("change password validates the request body")
    void changePasswordValidationFailure() throws Exception {
        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setNewPassword("123");

        mockMvc.perform(put("/auth/password/7")
                        .contentType(APPLICATION_JSON)
                        .content(writeJson(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.newPassword").value("Password must be at least 6 characters"));
    }

    @Test
    @DisplayName("change password delegates to the service")
    void changePasswordSuccess() throws Exception {
        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setNewPassword("ResetPass@1");

        mockMvc.perform(put("/auth/password/7")
                        .contentType(APPLICATION_JSON)
                        .content(writeJson(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Password updated successfully"));

        verify(authService).changePassword(7, "ResetPass@1");
    }

    @Test
    @DisplayName("deactivate account delegates to the service")
    void deactivateSuccess() throws Exception {
        mockMvc.perform(put("/auth/deactivate/13"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Account deactivated successfully"));

        verify(authService).deactivateAccount(13);
    }

    @Test
    @DisplayName("admin registration returns 201 on success")
    void registerAdminSuccess() throws Exception {
        RegisterAdminRequest request = new RegisterAdminRequest();
        request.setFullName("Admin User");
        request.setEmail("admin@test.com");
        request.setPassword("AdminPass@1");
        request.setAdminCode("SECRET");

        when(authService.registerAdmin(any(RegisterAdminRequest.class), eq("SECRET")))
                .thenReturn(user(21, "admin@test.com", "Admin", true, true));

        mockMvc.perform(post("/auth/admin/register")
                        .contentType(APPLICATION_JSON)
                        .content(writeJson(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId").value(21))
                .andExpect(jsonPath("$.email").value("admin@test.com"));
    }

    @Test
    @DisplayName("admin registration returns 401 when service rejects the request")
    void registerAdminFailure() throws Exception {
        RegisterAdminRequest request = new RegisterAdminRequest();
        request.setFullName("Admin User");
        request.setEmail("admin@test.com");
        request.setPassword("AdminPass@1");
        request.setAdminCode("SECRET");

        when(authService.registerAdmin(any(RegisterAdminRequest.class), eq("SECRET")))
                .thenThrow(new RuntimeException("Invalid admin code"));

        mockMvc.perform(post("/auth/admin/register")
                        .contentType(APPLICATION_JSON)
                        .content(writeJson(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Invalid admin code"));
    }

    @Test
    @DisplayName("google/complete validates incoming role values")
    void completeGoogleLoginValidationFailure() throws Exception {
        GoogleLoginCompletionRequest request = new GoogleLoginCompletionRequest();
        request.setEmail("user@test.com");
        request.setFullName("Google User");
        request.setProvider("google");
        request.setRole("Admin");

        mockMvc.perform(post("/auth/google/complete")
                        .contentType(APPLICATION_JSON)
                        .content(writeJson(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.role").value("Role must be Patient or Provider"));
    }

    @Test
    @DisplayName("google/complete returns a token for the chosen role")
    void completeGoogleLoginSuccess() throws Exception {
        GoogleLoginCompletionRequest request = new GoogleLoginCompletionRequest();
        request.setEmail("google@test.com");
        request.setFullName("Google User");
        request.setProvider("google");
        request.setPicture("pic");
        request.setRole("Provider");

        when(authService.findOrCreateGoogleUser("google@test.com", "Google User", "pic", "google", "Provider"))
                .thenReturn(user(22, "google@test.com", "Provider", true, true));
        when(jwtUtil.generateToken("google@test.com", "Provider", 22)).thenReturn("google-token");

        mockMvc.perform(post("/auth/google/complete")
                        .contentType(APPLICATION_JSON)
                        .content(writeJson(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("google-token"))
                .andExpect(jsonPath("$.role").value("Provider"));
    }

    @Test
    @DisplayName("add-phone validates required phone values")
    void addPhoneValidationFailure() throws Exception {
        AddPhoneRequest request = new AddPhoneRequest();
        request.setEmail("riya@test.com");
        request.setPhone(" ");

        mockMvc.perform(post("/auth/add-phone")
                        .contentType(APPLICATION_JSON)
                        .content(writeJson(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.phone").value("Phone number is required"));
    }

    @Test
    @DisplayName("add-phone saves the phone and sends OTP")
    void addPhoneSuccess() throws Exception {
        AddPhoneRequest request = new AddPhoneRequest();
        request.setEmail("riya@test.com");
        request.setPhone("9876543210");

        User user = user(23, "riya@test.com", "Patient", true, false);
        when(authService.getUserByEmail("riya@test.com")).thenReturn(user);
        when(authService.updateProfile(eq(23), any(User.class))).thenReturn(user);

        mockMvc.perform(post("/auth/add-phone")
                        .contentType(APPLICATION_JSON)
                        .content(writeJson(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.otpSent").value(true));

        verify(authService).sendOtp("riya@test.com");
    }

    @Test
    @DisplayName("forgot-password sends the reset email for valid payloads")
    void forgotPasswordSuccess() throws Exception {
        EmailRequest request = new EmailRequest();
        request.setEmail("riya@test.com");

        mockMvc.perform(post("/auth/forgot-password")
                        .contentType(APPLICATION_JSON)
                        .content(writeJson(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sent").value(true))
                .andExpect(jsonPath("$.message").value("Reset link sent to your email"));

        verify(authService).forgotPassword("riya@test.com");
    }

    @Test
    @DisplayName("forgot-password masks service failures with a generic response")
    void forgotPasswordMasksExceptions() throws Exception {
        EmailRequest request = new EmailRequest();
        request.setEmail("riya@test.com");
        doThrow(new RuntimeException("Mail failed")).when(authService).forgotPassword("riya@test.com");

        mockMvc.perform(post("/auth/forgot-password")
                        .contentType(APPLICATION_JSON)
                        .content(writeJson(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sent").value(true))
                .andExpect(jsonPath("$.message")
                        .value("If this email is registered, a reset link has been sent"));
    }

    @Test
    @DisplayName("verify-reset-otp validates the OTP format")
    void verifyResetOtpValidationFailure() throws Exception {
        ResetPasswordVerificationRequest request = new ResetPasswordVerificationRequest();
        request.setToken("token");
        request.setOtp("12");

        mockMvc.perform(post("/auth/verify-reset-otp")
                        .contentType(APPLICATION_JSON)
                        .content(writeJson(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.otp").value("OTP must be a 6-digit number"));
    }

    @Test
    @DisplayName("verify-reset-otp delegates to the service")
    void verifyResetOtpSuccess() throws Exception {
        ResetPasswordVerificationRequest request = new ResetPasswordVerificationRequest();
        request.setToken("reset-token");
        request.setOtp("123456");

        mockMvc.perform(post("/auth/verify-reset-otp")
                        .contentType(APPLICATION_JSON)
                        .content(writeJson(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verified").value(true));

        verify(authService).verifyResetOtp("reset-token", "123456");
    }

    @Test
    @DisplayName("reset-password validates minimum password length")
    void resetPasswordValidationFailure() throws Exception {
        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setToken("reset-token");
        request.setNewPassword("123");

        mockMvc.perform(post("/auth/reset-password")
                        .contentType(APPLICATION_JSON)
                        .content(writeJson(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.newPassword").value("Password must be at least 6 characters"));
    }

    @Test
    @DisplayName("reset-password delegates to the service")
    void resetPasswordSuccess() throws Exception {
        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setToken("reset-token");
        request.setNewPassword("ResetPass@1");

        mockMvc.perform(post("/auth/reset-password")
                        .contentType(APPLICATION_JSON)
                        .content(writeJson(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        verify(authService).resetPassword("reset-token", "ResetPass@1");
    }

    @Test
    @DisplayName("users endpoint returns every user")
    void getAllUsersSuccess() throws Exception {
        when(authService.getAllUsers()).thenReturn(List.of(
                user(31, "one@test.com", "Patient", true, true),
                user(32, "two@test.com", "Provider", true, true)
        ));

        mockMvc.perform(get("/auth/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].userId").value(31))
                .andExpect(jsonPath("$[1].role").value("Provider"));
    }

    @Test
    @DisplayName("users by role endpoint delegates to the service")
    void getUsersByRoleSuccess() throws Exception {
        when(authService.getUsersByRole("Patient")).thenReturn(List.of(
                user(33, "patient@test.com", "Patient", true, true)
        ));

        mockMvc.perform(get("/auth/users/role/Patient"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].email").value("patient@test.com"));
    }

    @Test
    @DisplayName("reactivate endpoint delegates to the service")
    void reactivateSuccess() throws Exception {
        mockMvc.perform(put("/auth/reactivate/41"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Account reactivated successfully"));

        verify(authService).reactivateAccount(41);
    }

    private LoginRequest loginRequest() {
        LoginRequest request = new LoginRequest();
        request.setEmail("riya@test.com");
        request.setPassword("Password@1");
        return request;
    }

    private User user(int userId, String email, String role, boolean active, boolean verified) {
        return User.builder()
                .userId(userId)
                .fullName("Test User")
                .email(email)
                .passwordHash("hash")
                .phone("9876543210")
                .role(role)
                .isActive(active)
                .verified(verified)
                .build();
    }

    private String writeJson(Object value) throws Exception {
        return objectMapper.writeValueAsString(value);
    }
}
