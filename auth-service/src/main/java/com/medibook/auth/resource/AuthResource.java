package com.medibook.auth.resource;


import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
import com.medibook.auth.exception.BadRequestException;
import com.medibook.auth.security.JwtUtil;
import com.medibook.auth.service.AuthService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/auth")
// @CrossOrigin REMOVED — CORS is handled entirely by the API Gateway (application.yml globalcors).
// Adding @CrossOrigin here causes duplicate Access-Control-Allow-Origin headers → browser rejects.
public class AuthResource {

    private static final String KEY_MESSAGE = "message";
    private static final String KEY_USER_ID = "userId";
    private static final String KEY_EMAIL = "email";
    private static final String KEY_TOKEN = "token";
    private static final String KEY_FULL_NAME = "fullName";
    private static final String KEY_OTP_SENT = "otpSent";

    private final AuthService authService;
    private final JwtUtil jwtUtil;

    @Value("${app.admin.secret.code}")
    private String adminSecretCode;

    public AuthResource(AuthService authService, JwtUtil jwtUtil) {
        this.authService = authService;
        this.jwtUtil = jwtUtil;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request) {
        System.out.println("reaching register end point in auth service");
        User user = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of(
                        KEY_MESSAGE, "Registration successful",
                        KEY_USER_ID, user.getUserId(),
                        "role", user.getRole()
                ));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {

        authService.login(request);

        User user = authService.getUserByEmail(request.getEmail());

        // Phone check - skip for admin
        if (!user.getRole().equals("Admin")
                && (user.getPhone() == null || user.getPhone().trim().isEmpty())
                && user.getProvider() == null) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of(
                        KEY_MESSAGE, "Phone number required",
                        "requiresPhone", true,
                        KEY_EMAIL, request.getEmail()
                    ));
        }

        // if already verified, skip OTP and return token directly.
        if (user.isVerified()) {
            String token = jwtUtil.generateToken(
                user.getEmail(),
                user.getRole(),
                user.getUserId()
            );
            return ResponseEntity.ok(Map.of(
                KEY_TOKEN,  token,
                KEY_USER_ID, user.getUserId(),
                "role",     user.getRole(),
                KEY_FULL_NAME, user.getFullName(),
                KEY_EMAIL, user.getEmail(),
                KEY_MESSAGE, "Login successful"
            ));
        }
        
        // First-time login - send OTP.
        authService.sendOtp(request.getEmail());
        return ResponseEntity.ok(Map.of(
                KEY_OTP_SENT, true,
                KEY_EMAIL, request.getEmail(),
                KEY_MESSAGE, "OTP sent to your email"
        ));
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<?> verifyOtp(@Valid @RequestBody OtpVerificationRequest request) {
        String email = request.getEmail();
        String otp = request.getOtp();

        authService.verifyOtp(email, otp);

        User user = authService.getUserByEmail(email);

        // mark user as verified after first successful OTP.
        if (!user.isVerified()) {
            user.setVerified(true);
            authService.updateProfile(user.getUserId(), user);
        }
        String token = jwtUtil.generateToken(
                user.getEmail(),
                user.getRole(),
                user.getUserId()
        );

        return ResponseEntity.ok(Map.of(
                KEY_TOKEN, token,
                KEY_USER_ID, user.getUserId(),
                "role",     user.getRole(),
                KEY_FULL_NAME, user.getFullName(),
                KEY_MESSAGE, "Login successful"
        ));
    }

    @PostMapping("/resend-otp")
    public ResponseEntity<?> resendOtp(@Valid @RequestBody EmailRequest request) {
        authService.sendOtp(request.getEmail());
        return ResponseEntity.ok(Map.of(
                KEY_OTP_SENT, true,
                KEY_MESSAGE, "New OTP sent to your email"
        ));
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(
            @RequestHeader("Authorization") String authHeader) {
        String token = extractBearerToken(authHeader);
        authService.logout(token);
        return ResponseEntity.ok(Map.of(KEY_MESSAGE, "Logged out successfully"));
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refresh(
            @RequestHeader("Authorization") String authHeader) {
        String token = extractBearerToken(authHeader);
        String newToken = authService.refreshToken(token);
        return ResponseEntity.ok(Map.of(KEY_TOKEN, newToken));
    }

    @GetMapping("/profile/{userId}")
    public ResponseEntity<User> getProfile(@PathVariable int userId) {
        return ResponseEntity.ok(authService.getUserById(userId));
    }

    @PutMapping("/profile/{userId}")
    public ResponseEntity<User> updateProfile(
            @PathVariable int userId,
            @RequestBody User updatedUser) {
        return ResponseEntity.ok(authService.updateProfile(userId, updatedUser));
    }

    @PutMapping("/password/{userId}")
    public ResponseEntity<?> changePassword(
            @PathVariable int userId,
            @Valid @RequestBody ChangePasswordRequest request) {
        authService.changePassword(userId, request.getNewPassword());
        return ResponseEntity.ok(Map.of(KEY_MESSAGE, "Password updated successfully"));
    }

    @PutMapping("/deactivate/{userId}")
    public ResponseEntity<?> deactivate(@PathVariable int userId) {
        authService.deactivateAccount(userId);
        return ResponseEntity.ok(Map.of(KEY_MESSAGE, "Account deactivated successfully"));
    }

    @PostMapping("/admin/register")
    public ResponseEntity<?> registerAdmin(@Valid @RequestBody RegisterAdminRequest request) {
        try {
            User admin = authService.registerAdmin(request, adminSecretCode);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(Map.of(
                            KEY_MESSAGE, "Admin account created successfully",
                            KEY_USER_ID, admin.getUserId(),
                            KEY_EMAIL, admin.getEmail()
                    ));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of(KEY_MESSAGE, e.getMessage()));
        }
    }

    @PostMapping("/google/complete")
    public ResponseEntity<?> completeGoogleLogin(@Valid @RequestBody GoogleLoginCompletionRequest request) {
        User user = authService.findOrCreateGoogleUser(
                request.getEmail(),
                request.getFullName(),
                request.getPicture(),
                request.getProvider(),
                request.getRole()
        );

        String token = jwtUtil.generateToken(
                user.getEmail(),
                user.getRole(),
                user.getUserId()
        );

        return ResponseEntity.ok(Map.of(
                KEY_TOKEN, token,
                KEY_USER_ID, user.getUserId(),
                "role",     user.getRole(),
                KEY_FULL_NAME, user.getFullName()
        ));
    }

    @PostMapping("/add-phone")
    public ResponseEntity<?> addPhone(@Valid @RequestBody AddPhoneRequest request) {
        User user = authService.getUserByEmail(request.getEmail());
        user.setPhone(request.getPhone());
        authService.updateProfile(user.getUserId(), user);
        authService.sendOtp(request.getEmail());

        return ResponseEntity.ok(Map.of(
                KEY_OTP_SENT, true,
                KEY_MESSAGE, "Phone saved and OTP sent to your email"
        ));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@Valid @RequestBody EmailRequest request) {
        try {
            authService.forgotPassword(request.getEmail().trim());
            return ResponseEntity.ok(Map.of(
                    "sent", true,
                    KEY_MESSAGE, "Reset link sent to your email"
            ));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of(
                    "sent", true,
                    KEY_MESSAGE, "If this email is registered, a reset link has been sent"
            ));
        }
    }

    @PostMapping("/verify-reset-otp")
    public ResponseEntity<?> verifyResetOtp(@Valid @RequestBody ResetPasswordVerificationRequest request) {
        authService.verifyResetOtp(request.getToken(), request.getOtp());

        return ResponseEntity.ok(Map.of(
                "verified", true,
                KEY_MESSAGE, "OTP verified. You can now reset your password."
        ));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request.getToken(), request.getNewPassword());

        return ResponseEntity.ok(Map.of(
                "success", true,
                KEY_MESSAGE, "Password reset successful. Please login with your new password."
        ));
    }

    // ── Admin User Management Endpoints ──────────────────────────────────

    /**
     * GET /auth/users — returns all users (Admin only)
     */
    @GetMapping("/users")
    public ResponseEntity<List<User>> getAllUsers() {
        return ResponseEntity.ok(authService.getAllUsers());
    }

    /**
     * GET /auth/users/role/{role} — returns users filtered by role
     * role values: Patient, Provider, Admin
     */
    @GetMapping("/users/role/{role}")
    public ResponseEntity<List<User>> getUsersByRole(@PathVariable String role) {
        return ResponseEntity.ok(authService.getUsersByRole(role));
    }

    /**
     * PUT /auth/reactivate/{userId} — re-enable a deactivated user account
     */
    @PutMapping("/reactivate/{userId}")
    public ResponseEntity<?> reactivate(@PathVariable int userId) {
        authService.reactivateAccount(userId);
        return ResponseEntity.ok(Map.of(KEY_MESSAGE, "Account reactivated successfully"));
    }

    private String extractBearerToken(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new BadRequestException("Authorization header must contain a Bearer token.");
        }

        String token = authHeader.substring(7).trim();
        if (token.isEmpty()) {
            throw new BadRequestException("Authorization header must contain a Bearer token.");
        }
        return token;
    }
}
