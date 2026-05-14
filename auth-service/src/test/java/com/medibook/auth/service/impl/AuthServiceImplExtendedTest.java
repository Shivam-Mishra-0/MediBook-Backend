package com.medibook.auth.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.medibook.auth.dto.request.RegisterAdminRequest;
import com.medibook.auth.entity.PasswordResetToken;
import com.medibook.auth.entity.User;
import com.medibook.auth.exception.BadRequestException;
import com.medibook.auth.exception.DuplicateResourceException;
import com.medibook.auth.exception.ResourceNotFoundException;
import com.medibook.auth.exception.UnauthorizedException;
import com.medibook.auth.repository.PasswordResetTokenRepository;
import com.medibook.auth.repository.UserRepository;
import com.medibook.auth.security.JwtUtil;
import com.medibook.otp.service.OtpService;

/**
 * Extended tests for AuthServiceImpl covering:
 * registerAdmin, findOrCreateGoogleUser, logout, validateToken,
 * refreshToken, getUserByEmail, getUserById, changePassword,
 * getAllUsers, getUsersByRole, reactivateAccount,
 * sendOtp, verifyOtp, forgotPassword, verifyResetOtp, resetPassword
 */
@MockitoSettings(strictness = Strictness.LENIENT)
@ExtendWith(MockitoExtension.class)
class AuthServiceImplExtendedTest {

    @Mock private UserRepository               userRepository;
    @Mock private PasswordEncoder              passwordEncoder;
    @Mock private JwtUtil                      jwtUtil;
    @Mock private OtpService                   otpService;
    @Mock private PasswordResetTokenRepository passwordResetTokenRepository;
    @Mock private JavaMailSender               mailSender;

    @InjectMocks
    private AuthServiceImpl authService;

    private User sampleUser;

    @BeforeEach
    void setUp() {
        sampleUser = User.builder()
                .userId(1)
                .fullName("Riya Sharma")
                .email("riya@medibook.com")
                .passwordHash("$2a$hashed")
                .phone("9876543210")
                .role("Patient")
                .isActive(true)
                .build();
    }

    /* ── registerAdmin() ────────────────────────────────────────── */

    @Test
    @DisplayName("registerAdmin: success with correct admin code")
    void registerAdmin_success() {
        RegisterAdminRequest req = new RegisterAdminRequest();
        req.setFullName("Admin User");
        req.setEmail("admin@medibook.com");
        req.setPassword("Admin@123");
        req.setAdminCode("SECRET");

        User adminUser = User.builder().userId(2).email("admin@medibook.com").role("Admin").build();
        when(userRepository.existsByEmail("admin@medibook.com")).thenReturn(false);
        when(passwordEncoder.encode("Admin@123")).thenReturn("$2a$adminHash");
        when(userRepository.save(any(User.class))).thenReturn(adminUser);

        User result = authService.registerAdmin(req, "SECRET");

        assertThat(result).isNotNull();
        assertThat(result.getEmail()).isEqualTo("admin@medibook.com");
    }

    @Test
    @DisplayName("registerAdmin: throws UnauthorizedException for wrong admin code")
    void registerAdmin_wrongCode() {
        RegisterAdminRequest req = new RegisterAdminRequest();
        req.setAdminCode("WRONG");

        assertThatThrownBy(() -> authService.registerAdmin(req, "SECRET"))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("Invalid admin code");
    }

    @Test
    @DisplayName("registerAdmin: throws DuplicateResourceException for duplicate email")
    void registerAdmin_duplicateEmail() {
        RegisterAdminRequest req = new RegisterAdminRequest();
        req.setEmail("dup@medibook.com");
        req.setAdminCode("SECRET");

        when(userRepository.existsByEmail("dup@medibook.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.registerAdmin(req, "SECRET"))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("dup@medibook.com");
    }

    /* ── findOrCreateGoogleUser() ─────────────────────────────── */

    @Test
    @DisplayName("findOrCreateGoogleUser: returns existing user if email already registered")
    void findOrCreateGoogleUser_existingUser() {
        when(userRepository.findByEmail("riya@medibook.com"))
                .thenReturn(Optional.of(sampleUser));

        User result = authService.findOrCreateGoogleUser(
                "riya@medibook.com", "Riya Sharma", "pic.jpg", "google", "Patient");

        assertThat(result).isEqualTo(sampleUser);
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("findOrCreateGoogleUser: creates new user when not found")
    void findOrCreateGoogleUser_newUser() {
        User newUser = User.builder().email("new@medibook.com").role("Provider").build();
        when(userRepository.findByEmail("new@medibook.com")).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenReturn(newUser);

        User result = authService.findOrCreateGoogleUser(
                "new@medibook.com", "New User", "pic.jpg", "google", "Provider");

        assertThat(result).isNotNull();
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("findOrCreateGoogleUser: throws BadRequestException for invalid role")
    void findOrCreateGoogleUser_invalidRole() {
        when(userRepository.findByEmail("bad@medibook.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.findOrCreateGoogleUser(
                "bad@medibook.com", "Bad User", "pic.jpg", "google", "InvalidRole"))
                .isInstanceOf(com.medibook.auth.exception.BadRequestException.class)
                .hasMessageContaining("Invalid role");
    }

    /* ── logout() ───────────────────────────────────────────────── */

    @Test
    @DisplayName("logout: completes without throwing (stateless JWT)")
    void logout_noOp() {
        // Should not throw
        authService.logout("some.jwt.token");
    }

    /* ── validateToken() ────────────────────────────────────────── */

    @Test
    @DisplayName("validateToken: returns true for valid token")
    void validateToken_valid() {
        when(jwtUtil.validateToken("valid.token")).thenReturn(true);
        assertThat(authService.validateToken("valid.token")).isTrue();
    }

    @Test
    @DisplayName("validateToken: returns false for invalid token")
    void validateToken_invalid() {
        when(jwtUtil.validateToken("bad.token")).thenReturn(false);
        assertThat(authService.validateToken("bad.token")).isFalse();
    }

    /* ── refreshToken() ─────────────────────────────────────────── */

    @Test
    @DisplayName("refreshToken: returns new token for valid token")
    void refreshToken_success() {
        when(jwtUtil.validateToken("old.token")).thenReturn(true);
        when(jwtUtil.extractEmail("old.token")).thenReturn("riya@medibook.com");
        when(jwtUtil.extractRole("old.token")).thenReturn("Patient");
        when(jwtUtil.extractUserId("old.token")).thenReturn(1);
        when(jwtUtil.generateToken("riya@medibook.com", "Patient", 1)).thenReturn("new.token");

        String result = authService.refreshToken("old.token");

        assertThat(result).isEqualTo("new.token");
    }

    @Test
    @DisplayName("refreshToken: throws UnauthorizedException for invalid token")
    void refreshToken_invalid() {
        when(jwtUtil.validateToken("bad.token")).thenReturn(false);

        assertThatThrownBy(() -> authService.refreshToken("bad.token"))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("Invalid or expired token");
    }

    /* ── getUserByEmail() ────────────────────────────────────────── */

    @Test
    @DisplayName("getUserByEmail: returns user when found")
    void getUserByEmail_found() {
        when(userRepository.findByEmail("riya@medibook.com")).thenReturn(Optional.of(sampleUser));
        assertThat(authService.getUserByEmail("riya@medibook.com")).isEqualTo(sampleUser);
    }

    @Test
    @DisplayName("getUserByEmail: throws ResourceNotFoundException when not found")
    void getUserByEmail_notFound() {
        when(userRepository.findByEmail("ghost@medibook.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.getUserByEmail("ghost@medibook.com"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    /* ── getUserById() ───────────────────────────────────────────── */

    @Test
    @DisplayName("getUserById: returns user when found")
    void getUserById_found() {
        when(userRepository.findByUserId(1)).thenReturn(Optional.of(sampleUser));
        assertThat(authService.getUserById(1)).isEqualTo(sampleUser);
    }

    @Test
    @DisplayName("getUserById: throws ResourceNotFoundException when not found")
    void getUserById_notFound() {
        when(userRepository.findByUserId(999)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.getUserById(999))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    /* ── changePassword() ────────────────────────────────────────── */

    @Test
    @DisplayName("changePassword: success — encodes and saves new password")
    void changePassword_success() {
        when(userRepository.findByUserId(1)).thenReturn(Optional.of(sampleUser));
        when(passwordEncoder.encode("NewPass@1")).thenReturn("$2a$newHash");
        when(userRepository.save(any(User.class))).thenReturn(sampleUser);

        authService.changePassword(1, "NewPass@1");

        verify(passwordEncoder).encode("NewPass@1");
        verify(userRepository).save(argThat(u -> u.getPasswordHash().equals("$2a$newHash")));
    }

    @Test
    @DisplayName("changePassword: throws BadRequestException for null password")
    void changePassword_nullPassword() {
        when(userRepository.findByUserId(1)).thenReturn(Optional.of(sampleUser));

        assertThatThrownBy(() -> authService.changePassword(1, null))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("cannot be empty");
    }

    @Test
    @DisplayName("changePassword: throws BadRequestException for blank password")
    void changePassword_blankPassword() {
        when(userRepository.findByUserId(1)).thenReturn(Optional.of(sampleUser));

        assertThatThrownBy(() -> authService.changePassword(1, "  "))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("cannot be empty");
    }

    @Test
    @DisplayName("changePassword: throws BadRequestException for password shorter than 6 chars")
    void changePassword_tooShort() {
        when(userRepository.findByUserId(1)).thenReturn(Optional.of(sampleUser));

        assertThatThrownBy(() -> authService.changePassword(1, "abc"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("at least 6 characters");
    }

    @Test
    @DisplayName("changePassword: throws ResourceNotFoundException for unknown user")
    void changePassword_userNotFound() {
        when(userRepository.findByUserId(999)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.changePassword(999, "NewPass@1"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    /* ── getAllUsers() ───────────────────────────────────────────── */

    @Test
    @DisplayName("getAllUsers: returns list from repository")
    void getAllUsers() {
        when(userRepository.findAll()).thenReturn(List.of(sampleUser));
        List<User> result = authService.getAllUsers();
        assertThat(result).hasSize(1).contains(sampleUser);
    }

    /* ── getUsersByRole() ────────────────────────────────────────── */

    @Test
    @DisplayName("getUsersByRole: filters users by role")
    void getUsersByRole() {
        when(userRepository.findAllByRole("Patient")).thenReturn(List.of(sampleUser));
        List<User> result = authService.getUsersByRole("Patient");
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getRole()).isEqualTo("Patient");
    }

    /* ── reactivateAccount() ─────────────────────────────────────── */

    @Test
    @DisplayName("reactivateAccount: sets isActive=true and saves")
    void reactivateAccount_success() {
        sampleUser.setActive(false);
        when(userRepository.findByUserId(1)).thenReturn(Optional.of(sampleUser));
        when(userRepository.save(any(User.class))).thenReturn(sampleUser);

        authService.reactivateAccount(1);

        verify(userRepository).save(argThat(User::isActive));
    }

    /* ── sendOtp() / verifyOtp() ─────────────────────────────────── */

    @Test
    @DisplayName("sendOtp: delegates to OtpService")
    void sendOtp_delegates() {
        doNothing().when(otpService).generateAndSendOtp("riya@medibook.com");

        authService.sendOtp("riya@medibook.com");

        verify(otpService).generateAndSendOtp("riya@medibook.com");
    }

    @Test
    @DisplayName("verifyOtp: delegates to OtpService and returns result")
    void verifyOtp_delegates() {
        when(otpService.verifyOtp("riya@medibook.com", "123456")).thenReturn(true);

        boolean result = authService.verifyOtp("riya@medibook.com", "123456");

        assertThat(result).isTrue();
    }

    /* ── forgotPassword() ────────────────────────────────────────── */

    @Test
    @DisplayName("forgotPassword: success — saves token and sends email")
    void forgotPassword_success() {
        when(userRepository.findByEmail("riya@medibook.com")).thenReturn(Optional.of(sampleUser));
        doNothing().when(passwordResetTokenRepository).deleteAllByEmail(anyString());
        when(passwordResetTokenRepository.save(any(PasswordResetToken.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        doNothing().when(mailSender).send(any(SimpleMailMessage.class));

        authService.forgotPassword("riya@medibook.com");

        verify(passwordResetTokenRepository).deleteAllByEmail("riya@medibook.com");
        verify(passwordResetTokenRepository).save(any(PasswordResetToken.class));
    }

    @Test
    @DisplayName("forgotPassword: throws ResourceNotFoundException for unknown email")
    void forgotPassword_userNotFound() {
        when(userRepository.findByEmail("ghost@medibook.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.forgotPassword("ghost@medibook.com"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("forgotPassword: completes even if mail sending fails")
    void forgotPassword_emailFailsGracefully() {
        when(userRepository.findByEmail("riya@medibook.com")).thenReturn(Optional.of(sampleUser));
        doNothing().when(passwordResetTokenRepository).deleteAllByEmail(anyString());
        when(passwordResetTokenRepository.save(any(PasswordResetToken.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        org.mockito.Mockito.doThrow(new RuntimeException("SMTP error"))
                .when(mailSender).send(any(SimpleMailMessage.class));

        // Should NOT throw
        authService.forgotPassword("riya@medibook.com");
    }

    /* ── verifyResetOtp() ────────────────────────────────────────── */

    private PasswordResetToken validResetToken(boolean used, boolean expired) {
        PasswordResetToken rt = new PasswordResetToken();
        rt.setEmail("riya@medibook.com");
        rt.setToken("reset-token-abc");
        rt.setOtp("654321");
        rt.setUsed(used);
        rt.setExpiresAt(expired
                ? LocalDateTime.now().minusMinutes(1)
                : LocalDateTime.now().plusMinutes(15));
        return rt;
    }

    @Test
    @DisplayName("verifyResetOtp: success — passes token and OTP validation")
    void verifyResetOtp_success() {
        PasswordResetToken rt = validResetToken(false, false);
        when(passwordResetTokenRepository.findByToken("reset-token-abc"))
                .thenReturn(Optional.of(rt));
        when(passwordResetTokenRepository.save(any(PasswordResetToken.class))).thenReturn(rt);

        authService.verifyResetOtp("reset-token-abc", "654321");

        verify(passwordResetTokenRepository).save(any(PasswordResetToken.class));
    }

    @Test
    @DisplayName("verifyResetOtp: throws BadRequestException for invalid token")
    void verifyResetOtp_invalidToken() {
        when(passwordResetTokenRepository.findByToken("bad-token"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.verifyResetOtp("bad-token", "123456"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Invalid or expired reset link");
    }

    @Test
    @DisplayName("verifyResetOtp: throws BadRequestException if token already used")
    void verifyResetOtp_alreadyUsed() {
        PasswordResetToken rt = validResetToken(true, false);
        when(passwordResetTokenRepository.findByToken("reset-token-abc"))
                .thenReturn(Optional.of(rt));

        assertThatThrownBy(() -> authService.verifyResetOtp("reset-token-abc", "654321"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("already been used");
    }

    @Test
    @DisplayName("verifyResetOtp: throws BadRequestException if token expired")
    void verifyResetOtp_expired() {
        PasswordResetToken rt = validResetToken(false, true);
        when(passwordResetTokenRepository.findByToken("reset-token-abc"))
                .thenReturn(Optional.of(rt));

        assertThatThrownBy(() -> authService.verifyResetOtp("reset-token-abc", "654321"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("expired");

        verify(passwordResetTokenRepository).delete(rt);
    }

    @Test
    @DisplayName("verifyResetOtp: throws BadRequestException for wrong OTP")
    void verifyResetOtp_wrongOtp() {
        PasswordResetToken rt = validResetToken(false, false);
        when(passwordResetTokenRepository.findByToken("reset-token-abc"))
                .thenReturn(Optional.of(rt));

        assertThatThrownBy(() -> authService.verifyResetOtp("reset-token-abc", "000000"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Invalid OTP");
    }

    /* ── resetPassword() ─────────────────────────────────────────── */

    @Test
    @DisplayName("resetPassword: success — encodes and saves new password, deletes token")
    void resetPassword_success() {
        PasswordResetToken rt = validResetToken(false, false);
        when(passwordResetTokenRepository.findByToken("reset-token-abc"))
                .thenReturn(Optional.of(rt));
        when(userRepository.findByEmail("riya@medibook.com")).thenReturn(Optional.of(sampleUser));
        when(passwordEncoder.encode("NewPass@123")).thenReturn("$2a$newHash");
        when(userRepository.save(any(User.class))).thenReturn(sampleUser);

        authService.resetPassword("reset-token-abc", "NewPass@123");

        verify(passwordEncoder).encode("NewPass@123");
        verify(userRepository).save(any(User.class));
        verify(passwordResetTokenRepository).delete(rt);
    }

    @Test
    @DisplayName("resetPassword: throws BadRequestException for invalid token")
    void resetPassword_invalidToken() {
        when(passwordResetTokenRepository.findByToken("bad")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.resetPassword("bad", "Pass@1"))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("resetPassword: throws BadRequestException if token expired")
    void resetPassword_expired() {
        PasswordResetToken rt = validResetToken(false, true);
        when(passwordResetTokenRepository.findByToken("reset-token-abc"))
                .thenReturn(Optional.of(rt));

        assertThatThrownBy(() -> authService.resetPassword("reset-token-abc", "Pass@1"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("expired");
    }

    @Test
    @DisplayName("resetPassword: throws BadRequestException for null new password")
    void resetPassword_nullPassword() {
        PasswordResetToken rt = validResetToken(false, false);
        when(passwordResetTokenRepository.findByToken("reset-token-abc"))
                .thenReturn(Optional.of(rt));

        assertThatThrownBy(() -> authService.resetPassword("reset-token-abc", null))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("cannot be empty");
    }

    @Test
    @DisplayName("resetPassword: throws BadRequestException for password shorter than 6 chars")
    void resetPassword_tooShort() {
        PasswordResetToken rt = validResetToken(false, false);
        when(passwordResetTokenRepository.findByToken("reset-token-abc"))
                .thenReturn(Optional.of(rt));

        assertThatThrownBy(() -> authService.resetPassword("reset-token-abc", "abc"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("at least 6 characters");
    }

    @Test
    @DisplayName("resetPassword: throws ResourceNotFoundException when reset email has no user")
    void resetPassword_userNotFound() {
        PasswordResetToken rt = validResetToken(false, false);
        when(passwordResetTokenRepository.findByToken("reset-token-abc"))
                .thenReturn(Optional.of(rt));
        when(userRepository.findByEmail("riya@medibook.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.resetPassword("reset-token-abc", "ValidPass@1"))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
