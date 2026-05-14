package com.medibook.auth.dto.request;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

class RequestValidationTest {

    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    @DisplayName("RegisterRequest enforces full name, email, password, and role")
    void registerRequestValidation() {
        RegisterRequest request = new RegisterRequest();
        request.setFullName("");
        request.setEmail("bad-email");
        request.setPassword("123");
        request.setRole("Admin");

        assertThat(messagesByField(validator.validate(request))).containsExactlyInAnyOrderEntriesOf(Map.of(
                "fullName", "Full name is required",
                "email", "Valid email is required",
                "password", "Password must be at least 6 characters",
                "role", "Role must be Patient or Provider"
        ));
    }

    @Test
    @DisplayName("LoginRequest requires email and password")
    void loginRequestValidation() {
        LoginRequest request = new LoginRequest();
        request.setEmail(null);
        request.setPassword("");

        assertThat(messagesByField(validator.validate(request))).containsEntry("email", "Email is required")
                .containsEntry("password", "Password is required");
    }

    @Test
    @DisplayName("RegisterAdminRequest requires valid admin registration details")
    void registerAdminRequestValidation() {
        RegisterAdminRequest request = new RegisterAdminRequest();
        request.setFullName("");
        request.setEmail("wrong");
        request.setPassword("123");
        request.setAdminCode("");

        assertThat(messagesByField(validator.validate(request))).containsExactlyInAnyOrderEntriesOf(Map.of(
                "fullName", "Full name is required",
                "email", "Email should be valid",
                "password", "Password must be at least 6 characters",
                "adminCode", "Admin code is required"
        ));
    }

    @Test
    @DisplayName("EmailRequest requires a valid email")
    void emailRequestValidation() {
        EmailRequest request = new EmailRequest();
        request.setEmail("bad");

        assertThat(messagesByField(validator.validate(request))).containsEntry("email", "Valid email is required");
    }

    @Test
    @DisplayName("AddPhoneRequest requires email and phone")
    void addPhoneRequestValidation() {
        AddPhoneRequest request = new AddPhoneRequest();
        request.setEmail("phone");
        request.setPhone(" ");

        assertThat(messagesByField(validator.validate(request))).containsEntry("email", "Valid email is required")
                .containsEntry("phone", "Phone number is required");
    }

    @Test
    @DisplayName("ChangePasswordRequest requires a minimum password length")
    void changePasswordRequestValidation() {
        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setNewPassword("123");

        assertThat(messagesByField(validator.validate(request))).containsEntry(
                "newPassword",
                "Password must be at least 6 characters"
        );
    }

    @Test
    @DisplayName("GoogleLoginCompletionRequest enforces role and required fields")
    void googleLoginCompletionRequestValidation() {
        GoogleLoginCompletionRequest request = new GoogleLoginCompletionRequest();
        request.setEmail("google");
        request.setFullName("");
        request.setProvider("");
        request.setRole("Admin");

        assertThat(messagesByField(validator.validate(request))).containsExactlyInAnyOrderEntriesOf(Map.of(
                "email", "Valid email is required",
                "fullName", "Full name is required",
                "provider", "Provider is required",
                "role", "Role must be Patient or Provider"
        ));
    }

    @Test
    @DisplayName("OtpVerificationRequest requires valid email and 6-digit OTP")
    void otpVerificationRequestValidation() {
        OtpVerificationRequest request = new OtpVerificationRequest();
        request.setEmail("otp");
        request.setOtp("1234");

        assertThat(messagesByField(validator.validate(request))).containsEntry("email", "Valid email is required")
                .containsEntry("otp", "OTP must be a 6-digit number");
    }

    @Test
    @DisplayName("ResetPasswordVerificationRequest requires token and 6-digit OTP")
    void resetPasswordVerificationRequestValidation() {
        ResetPasswordVerificationRequest request = new ResetPasswordVerificationRequest();
        request.setToken("");
        request.setOtp("abc");

        assertThat(messagesByField(validator.validate(request))).containsEntry("token", "Token is required")
                .containsEntry("otp", "OTP must be a 6-digit number");
    }

    @Test
    @DisplayName("ResetPasswordRequest requires token and minimum password length")
    void resetPasswordRequestValidation() {
        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setToken(" ");
        request.setNewPassword("123");

        assertThat(messagesByField(validator.validate(request))).containsEntry("token", "Token is required")
                .containsEntry("newPassword", "Password must be at least 6 characters");
    }

    private Map<String, String> messagesByField(Set<? extends ConstraintViolation<?>> violations) {
        return violations.stream().collect(Collectors.toMap(
                violation -> violation.getPropertyPath().toString(),
                ConstraintViolation::getMessage,
                (first, second) -> first
        ));
    }
}
