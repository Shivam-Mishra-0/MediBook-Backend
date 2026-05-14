package com.medibook.auth.entity;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.medibook.auth.dto.request.LoginRequest;
import com.medibook.auth.dto.request.AddPhoneRequest;
import com.medibook.auth.dto.request.ChangePasswordRequest;
import com.medibook.auth.dto.request.EmailRequest;
import com.medibook.auth.dto.request.GoogleLoginCompletionRequest;
import com.medibook.auth.dto.request.OtpVerificationRequest;
import com.medibook.auth.dto.request.RegisterAdminRequest;
import com.medibook.auth.dto.request.RegisterRequest;
import com.medibook.auth.dto.request.ResetPasswordRequest;
import com.medibook.auth.dto.request.ResetPasswordVerificationRequest;
import com.medibook.auth.dto.response.AuthResponse;
import com.medibook.otp.entity.OtpToken;

import java.time.LocalDateTime;

class EntityAndDtoTest {

    /* ── User entity ──────────────────────────────────────────── */

    @Test
    @DisplayName("User: builder sets all fields correctly")
    void user_builder() {
        User user = User.builder()
                .userId(1)
                .fullName("Riya Sharma")
                .email("riya@medibook.com")
                .passwordHash("$2a$hash")
                .phone("9876543210")
                .role("Patient")
                .isActive(true)
                .verified(false)
                .provider("google")
                .profilePicUrl("http://pic.url")
                .build();

        assertThat(user.getUserId()).isEqualTo(1);
        assertThat(user.getFullName()).isEqualTo("Riya Sharma");
        assertThat(user.getEmail()).isEqualTo("riya@medibook.com");
        assertThat(user.getPasswordHash()).isEqualTo("$2a$hash");
        assertThat(user.getPhone()).isEqualTo("9876543210");
        assertThat(user.getRole()).isEqualTo("Patient");
        assertThat(user.isActive()).isTrue();
        assertThat(user.isVerified()).isFalse();
        assertThat(user.getProvider()).isEqualTo("google");
        assertThat(user.getProfilePicUrl()).isEqualTo("http://pic.url");
    }

    @Test
    @DisplayName("User: setters update fields")
    void user_setters() {
        User user = new User();
        user.setFullName("New Name");
        user.setEmail("new@test.com");
        user.setActive(false);
        user.setVerified(true);

        assertThat(user.getFullName()).isEqualTo("New Name");
        assertThat(user.getEmail()).isEqualTo("new@test.com");
        assertThat(user.isActive()).isFalse();
        assertThat(user.isVerified()).isTrue();
    }

    @Test
    @DisplayName("User: default isActive is true via builder")
    void user_defaultIsActive() {
        User user = User.builder().email("x@x.com").build();
        assertThat(user.isActive()).isTrue();
    }

    @Test
    @DisplayName("User: default verified is false via builder")
    void user_defaultVerified() {
        User user = User.builder().email("x@x.com").build();
        assertThat(user.isVerified()).isFalse();
    }

    @Test
    @DisplayName("User: prePersist sets createdAt")
    void user_prePersist() {
        User user = new User();
        user.prePersist();
        assertThat(user.getCreatedAt()).isNotNull();
    }

    /* ── PasswordResetToken entity ────────────────────────────── */

    @Test
    @DisplayName("PasswordResetToken: builder sets fields correctly")
    void passwordResetToken_builder() {
        PasswordResetToken rt = PasswordResetToken.builder()
                .email("riya@medibook.com")
                .token("abc-token")
                .otp("123456")
                .used(false)
                .build();

        assertThat(rt.getEmail()).isEqualTo("riya@medibook.com");
        assertThat(rt.getToken()).isEqualTo("abc-token");
        assertThat(rt.getOtp()).isEqualTo("123456");
        assertThat(rt.isUsed()).isFalse();
    }

    @Test
    @DisplayName("PasswordResetToken: setters update fields")
    void passwordResetToken_setters() {
        PasswordResetToken rt = new PasswordResetToken();
        rt.setEmail("test@test.com");
        rt.setOtp("999999");
        rt.setUsed(true);
        LocalDateTime expiry = LocalDateTime.now().plusMinutes(15);
        rt.setExpiresAt(expiry);

        assertThat(rt.getEmail()).isEqualTo("test@test.com");
        assertThat(rt.getOtp()).isEqualTo("999999");
        assertThat(rt.isUsed()).isTrue();
        assertThat(rt.getExpiresAt()).isEqualTo(expiry);
    }

    @Test
    @DisplayName("PasswordResetToken: prePersist sets 15 minute expiry")
    void passwordResetToken_prePersist() {
        PasswordResetToken token = new PasswordResetToken();
        LocalDateTime before = LocalDateTime.now().plusMinutes(14);

        token.prePersist();

        assertThat(token.getExpiresAt()).isAfter(before);
    }

    /* ── OtpToken entity ──────────────────────────────────────── */

    @Test
    @DisplayName("OtpToken: builder sets fields correctly")
    void otpToken_builder() {
        LocalDateTime expiry = LocalDateTime.now().plusMinutes(5);
        OtpToken token = OtpToken.builder()
                .email("otp@test.com")
                .otp("654321")
                .used(false)
                .expiresAt(expiry)
                .build();

        assertThat(token.getEmail()).isEqualTo("otp@test.com");
        assertThat(token.getOtp()).isEqualTo("654321");
        assertThat(token.isUsed()).isFalse();
        assertThat(token.getExpiresAt()).isEqualTo(expiry);
    }

    @Test
    @DisplayName("OtpToken: setters work correctly")
    void otpToken_setters() {
        OtpToken token = new OtpToken();
        token.setEmail("x@x.com");
        token.setOtp("111111");
        token.setUsed(true);

        assertThat(token.getEmail()).isEqualTo("x@x.com");
        assertThat(token.getOtp()).isEqualTo("111111");
        assertThat(token.isUsed()).isTrue();
    }

    @Test
    @DisplayName("OtpToken: prePersist sets 5 minute expiry")
    void otpToken_prePersist() {
        OtpToken token = new OtpToken();
        LocalDateTime before = LocalDateTime.now().plusMinutes(4);

        token.prePersist();

        assertThat(token.getExpiresAt()).isAfter(before);
    }

    /* ── DTOs ─────────────────────────────────────────────────── */

    @Test
    @DisplayName("RegisterRequest: getters and setters")
    void registerRequest_gettersSetters() {
        RegisterRequest req = new RegisterRequest();
        req.setFullName("Test User");
        req.setEmail("test@test.com");
        req.setPassword("Pass@123");
        req.setPhone("9999999999");
        req.setRole("Patient");

        assertThat(req.getFullName()).isEqualTo("Test User");
        assertThat(req.getEmail()).isEqualTo("test@test.com");
        assertThat(req.getPassword()).isEqualTo("Pass@123");
        assertThat(req.getPhone()).isEqualTo("9999999999");
        assertThat(req.getRole()).isEqualTo("Patient");
    }

    @Test
    @DisplayName("LoginRequest: getters and setters")
    void loginRequest_gettersSetters() {
        LoginRequest req = new LoginRequest();
        req.setEmail("login@test.com");
        req.setPassword("MyPass@123");

        assertThat(req.getEmail()).isEqualTo("login@test.com");
        assertThat(req.getPassword()).isEqualTo("MyPass@123");
    }

    @Test
    @DisplayName("RegisterAdminRequest: getters and setters")
    void registerAdminRequest_gettersSetters() {
        RegisterAdminRequest req = new RegisterAdminRequest();
        req.setFullName("Admin");
        req.setEmail("admin@test.com");
        req.setPassword("AdminPass@1");
        req.setAdminCode("SECRET");

        assertThat(req.getFullName()).isEqualTo("Admin");
        assertThat(req.getEmail()).isEqualTo("admin@test.com");
        assertThat(req.getPassword()).isEqualTo("AdminPass@1");
        assertThat(req.getAdminCode()).isEqualTo("SECRET");
    }

    @Test
    @DisplayName("EmailRequest: getters and setters")
    void emailRequest_gettersSetters() {
        EmailRequest request = new EmailRequest();
        request.setEmail("mail@test.com");

        assertThat(request.getEmail()).isEqualTo("mail@test.com");
    }

    @Test
    @DisplayName("OtpVerificationRequest: getters and setters")
    void otpVerificationRequest_gettersSetters() {
        OtpVerificationRequest request = new OtpVerificationRequest();
        request.setEmail("otp@test.com");
        request.setOtp("123456");

        assertThat(request.getEmail()).isEqualTo("otp@test.com");
        assertThat(request.getOtp()).isEqualTo("123456");
    }

    @Test
    @DisplayName("ChangePasswordRequest: getters and setters")
    void changePasswordRequest_gettersSetters() {
        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setNewPassword("NewPass@123");

        assertThat(request.getNewPassword()).isEqualTo("NewPass@123");
    }

    @Test
    @DisplayName("AddPhoneRequest: getters and setters")
    void addPhoneRequest_gettersSetters() {
        AddPhoneRequest request = new AddPhoneRequest();
        request.setEmail("phone@test.com");
        request.setPhone("9876543210");

        assertThat(request.getEmail()).isEqualTo("phone@test.com");
        assertThat(request.getPhone()).isEqualTo("9876543210");
    }

    @Test
    @DisplayName("GoogleLoginCompletionRequest: getters and setters")
    void googleLoginCompletionRequest_gettersSetters() {
        GoogleLoginCompletionRequest request = new GoogleLoginCompletionRequest();
        request.setEmail("google@test.com");
        request.setFullName("Google User");
        request.setPicture("http://image");
        request.setProvider("google");
        request.setRole("Patient");

        assertThat(request.getEmail()).isEqualTo("google@test.com");
        assertThat(request.getFullName()).isEqualTo("Google User");
        assertThat(request.getPicture()).isEqualTo("http://image");
        assertThat(request.getProvider()).isEqualTo("google");
        assertThat(request.getRole()).isEqualTo("Patient");
    }

    @Test
    @DisplayName("ResetPasswordVerificationRequest: getters and setters")
    void resetPasswordVerificationRequest_gettersSetters() {
        ResetPasswordVerificationRequest request = new ResetPasswordVerificationRequest();
        request.setToken("reset-token");
        request.setOtp("654321");

        assertThat(request.getToken()).isEqualTo("reset-token");
        assertThat(request.getOtp()).isEqualTo("654321");
    }

    @Test
    @DisplayName("ResetPasswordRequest: getters and setters")
    void resetPasswordRequest_gettersSetters() {
        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setToken("reset-token");
        request.setNewPassword("ResetPass@1");

        assertThat(request.getToken()).isEqualTo("reset-token");
        assertThat(request.getNewPassword()).isEqualTo("ResetPass@1");
    }

    @Test
    @DisplayName("AuthResponse: all-args constructor and getters")
    void authResponse_allArgs() {
        AuthResponse resp = new AuthResponse("tok", "Patient", 1, "Riya", "OK");

        assertThat(resp.getToken()).isEqualTo("tok");
        assertThat(resp.getRole()).isEqualTo("Patient");
        assertThat(resp.getUserId()).isEqualTo(1);
        assertThat(resp.getFullName()).isEqualTo("Riya");
        assertThat(resp.getMessage()).isEqualTo("OK");
    }

    @Test
    @DisplayName("AuthResponse: no-arg constructor + setters")
    void authResponse_noArg() {
        AuthResponse resp = new AuthResponse();
        resp.setToken("t");
        resp.setRole("Admin");
        assertThat(resp.getToken()).isEqualTo("t");
        assertThat(resp.getRole()).isEqualTo("Admin");
    }

    @Test
    @DisplayName("Security AuthResponse: all-args constructor and getters")
    void securityAuthResponse_allArgs() {
        com.medibook.auth.security.AuthResponse response =
                new com.medibook.auth.security.AuthResponse("tok", "Admin", 10, "Root", "OK");

        assertThat(response.getToken()).isEqualTo("tok");
        assertThat(response.getRole()).isEqualTo("Admin");
        assertThat(response.getUserId()).isEqualTo(10);
        assertThat(response.getFullName()).isEqualTo("Root");
        assertThat(response.getMessage()).isEqualTo("OK");
    }

    @Test
    @DisplayName("Security AuthResponse: no-arg constructor and setters")
    void securityAuthResponse_noArgs() {
        com.medibook.auth.security.AuthResponse response = new com.medibook.auth.security.AuthResponse();
        response.setToken("jwt");
        response.setRole("Patient");
        response.setUserId(22);
        response.setFullName("User");
        response.setMessage("Done");

        assertThat(response.getToken()).isEqualTo("jwt");
        assertThat(response.getRole()).isEqualTo("Patient");
        assertThat(response.getUserId()).isEqualTo(22);
        assertThat(response.getFullName()).isEqualTo("User");
        assertThat(response.getMessage()).isEqualTo("Done");
    }
}
