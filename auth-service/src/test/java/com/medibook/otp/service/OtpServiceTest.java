package com.medibook.otp.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
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

import com.medibook.auth.exception.BadRequestException;
import com.medibook.auth.exception.ResourceNotFoundException;
import com.medibook.auth.entity.User;
import com.medibook.auth.repository.UserRepository;
import com.medibook.otp.entity.OtpToken;
import com.medibook.otp.repository.OtpRepository;

@MockitoSettings(strictness = Strictness.LENIENT)
@ExtendWith(MockitoExtension.class)
class OtpServiceTest {

    @Mock private OtpRepository   otpRepository;
    @Mock private JavaMailSender  mailSender;
    @Mock private UserRepository  userRepository;

    @InjectMocks
    private OtpService otpService;

    private static final String EMAIL = "test@medibook.com";

    private User sampleUser;

    @BeforeEach
    void setUp() {
        sampleUser = User.builder()
                .userId(1)
                .fullName("Test User")
                .email(EMAIL)
                .passwordHash("$2a$hashed")
                .role("Patient")
                .isActive(true)
                .build();
    }

    /* ── generateAndSendOtp() ─────────────────────────────────── */

    @Test
    @DisplayName("generateAndSendOtp: success — saves OTP and sends email")
    void generateAndSendOtp_success() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(sampleUser));
        when(otpRepository.save(any(OtpToken.class))).thenAnswer(inv -> inv.getArgument(0));

        otpService.generateAndSendOtp(EMAIL);

        verify(otpRepository).deleteAllByEmail(EMAIL);
        verify(otpRepository).save(any(OtpToken.class));
        verify(mailSender).send(any(SimpleMailMessage.class));
    }

    @Test
    @DisplayName("generateAndSendOtp: throws ResourceNotFoundException if user not found")
    void generateAndSendOtp_userNotFound() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> otpService.generateAndSendOtp(EMAIL))
                .isInstanceOf(ResourceNotFoundException.class);

        verify(otpRepository, never()).save(any());
    }

    @Test
    @DisplayName("generateAndSendOtp: still saves OTP even if email sending fails")
    void generateAndSendOtp_emailFails_otpStillSaved() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(sampleUser));
        when(otpRepository.save(any(OtpToken.class))).thenAnswer(inv -> inv.getArgument(0));
        doThrow(new RuntimeException("SMTP error")).when(mailSender).send(any(SimpleMailMessage.class));

        // Should NOT throw — email failure is swallowed
        otpService.generateAndSendOtp(EMAIL);

        verify(otpRepository).save(any(OtpToken.class));
    }

    /* ── verifyOtp() ──────────────────────────────────────────── */

    @Test
    @DisplayName("verifyOtp: success — returns true and deletes used token")
    void verifyOtp_success() {
        OtpToken token = OtpToken.builder()
                .email(EMAIL)
                .otp("123456")
                .used(false)
                .expiresAt(LocalDateTime.now().plusMinutes(5))
                .build();

        when(otpRepository.findTopByEmailAndUsedFalseOrderByExpiresAtDesc(EMAIL))
                .thenReturn(Optional.of(token));

        boolean result = otpService.verifyOtp(EMAIL, "123456");

        assertThat(result).isTrue();
        verify(otpRepository).delete(token);
    }

    @Test
    @DisplayName("verifyOtp: throws BadRequestException if no OTP found")
    void verifyOtp_noOtpFound() {
        when(otpRepository.findTopByEmailAndUsedFalseOrderByExpiresAtDesc(EMAIL))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> otpService.verifyOtp(EMAIL, "123456"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("No OTP found");
    }

    @Test
    @DisplayName("verifyOtp: throws BadRequestException if OTP is expired")
    void verifyOtp_expired() {
        OtpToken token = OtpToken.builder()
                .email(EMAIL)
                .otp("123456")
                .used(false)
                .expiresAt(LocalDateTime.now().minusMinutes(1)) // already expired
                .build();

        when(otpRepository.findTopByEmailAndUsedFalseOrderByExpiresAtDesc(EMAIL))
                .thenReturn(Optional.of(token));

        assertThatThrownBy(() -> otpService.verifyOtp(EMAIL, "123456"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("expired");

        verify(otpRepository).delete(token);
    }

    @Test
    @DisplayName("verifyOtp: throws BadRequestException for wrong OTP code")
    void verifyOtp_wrongCode() {
        OtpToken token = OtpToken.builder()
                .email(EMAIL)
                .otp("123456")
                .used(false)
                .expiresAt(LocalDateTime.now().plusMinutes(5))
                .build();

        when(otpRepository.findTopByEmailAndUsedFalseOrderByExpiresAtDesc(EMAIL))
                .thenReturn(Optional.of(token));

        assertThatThrownBy(() -> otpService.verifyOtp(EMAIL, "999999"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Invalid OTP");
    }
}
