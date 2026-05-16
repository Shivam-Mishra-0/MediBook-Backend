package com.medibook.auth.service.impl;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.medibook.auth.dto.request.LoginRequest;
import com.medibook.auth.dto.request.RegisterAdminRequest;
import com.medibook.auth.dto.request.RegisterRequest;
import com.medibook.auth.dto.response.AuthResponse;
import com.medibook.auth.entity.PasswordResetToken;
import com.medibook.auth.entity.User;
import com.medibook.auth.exception.BadRequestException;
import com.medibook.auth.exception.DuplicateResourceException;
import com.medibook.auth.exception.ResourceNotFoundException;
import com.medibook.auth.exception.UnauthorizedException;
import com.medibook.auth.repository.PasswordResetTokenRepository;
import com.medibook.auth.repository.UserRepository;
import com.medibook.auth.security.JwtUtil;
import com.medibook.auth.security.TokenBlacklistService;
import com.medibook.auth.service.AuthService;
import com.medibook.otp.service.OtpService;

@Service
public class AuthServiceImpl implements AuthService {

    private static final String USER_ENTITY = "User";
    private static final String EMAIL_FIELD = "email";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final OtpService otpService;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final JavaMailSender mailSender;
    private final TokenBlacklistService tokenBlacklistService;
    private String frontendBaseUrl = "http://localhost:5173";

    public AuthServiceImpl(UserRepository userRepository, PasswordEncoder passwordEncoder,
                           JwtUtil jwtUtil, OtpService otpService,
                           PasswordResetTokenRepository passwordResetTokenRepository,
                           JavaMailSender mailSender,
                           TokenBlacklistService tokenBlacklistService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.otpService = otpService;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.mailSender = mailSender;
        this.tokenBlacklistService = tokenBlacklistService;
    }

    @Value("${app.frontend.base-url:http://localhost:5173}")
    public void setFrontendBaseUrl(String frontendBaseUrl) {
        this.frontendBaseUrl = normalizeBaseUrl(frontendBaseUrl);
    }

    //  register()
    @Override
    public User register(RegisterRequest request) {

        // Check duplicate email
        if (userRepository.existsByEmail(request.getEmail())) {
        	throw new DuplicateResourceException(
        		    USER_ENTITY, EMAIL_FIELD, request.getEmail()
        		);
        }

        User user = User.builder()
                .fullName(request.getFullName())
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .phone(request.getPhone())
                .role(request.getRole())
                .isActive(true)
                .build();

        return userRepository.save(user);
    }

    //  login()
    @Override
    public AuthResponse login(LoginRequest request) {

        User user = userRepository.findByEmail(request.getEmail())
        		.orElseThrow(() -> new ResourceNotFoundException(
        			    USER_ENTITY, EMAIL_FIELD, request.getEmail()
        			));

        if (!user.isActive()) {
        	throw new UnauthorizedException(
        		    "Your account has been deactivated. " +
        		    "Please contact admin to reactivate."
        		);
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
        	throw new UnauthorizedException(
        		    "Invalid email or password. Please try again."
        		);
        }

        String token = jwtUtil.generateToken(
                user.getEmail(),
                user.getRole(),
                user.getUserId()
        );

        return new AuthResponse(
                token,
                user.getRole(),
                user.getUserId(),
                user.getFullName(),
                "Login successful"
        );
    }
    
 // Add this method to AuthService.java
    public User registerAdmin(RegisterAdminRequest request, String adminSecretCode) {
        // Validate secret code
        if (!request.getAdminCode().equals(adminSecretCode)) {
            throw new UnauthorizedException("Invalid admin code");
        }

        // Check if email exists
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new DuplicateResourceException(USER_ENTITY, EMAIL_FIELD, request.getEmail());
        }

        // Create admin user (same logic as normal registration)
        User admin = User.builder()
                .fullName(request.getFullName())
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role("Admin")
                .phone("")
                .isActive(true)
                .provider(null)
                .build();

        return userRepository.save(admin);
    }

    // Google OAuth2 â€” find existing user or create new one with chosen role
    @Override
    public User findOrCreateGoogleUser(String email, String fullName,
                                       String picture, String provider, String role) {

        // If user already exists (e.g. came back and picked role again) just return them
        return userRepository.findByEmail(email).orElseGet(() -> {

            // Validate role â€” only Patient or Provider allowed via Google
            if (!role.equals("Patient") && !role.equals("Provider")) {
                throw new BadRequestException("Invalid role selected. Must be Patient or Provider.");
            }

            User newUser = User.builder()
                    .email(email)
                    .fullName(fullName)
                    .passwordHash("")         // no password for Google users
                    .phone("")
                    .role(role)               // role chosen by user on select-role page
                    .provider(provider)       // "google"
                    .isActive(true)
                    .verified(true)
                    .profilePicUrl(picture)
                    .build();

            return userRepository.save(newUser);
        });
    }

    // logout()
    @Override
    public void logout(String token) {
        if (!jwtUtil.validateToken(token)) {
            return;
        }

        tokenBlacklistService.blacklistToken(token, jwtUtil.extractExpiration(token));
    }

    // validateToken()
    @Override
    public boolean validateToken(String token) {
        return jwtUtil.validateToken(token) && !tokenBlacklistService.isBlacklisted(token);
    }

    //  refreshToken()
    @Override
    public String refreshToken(String token) {
        if (!validateToken(token)) {
        	throw new UnauthorizedException(
        		    "Invalid or expired token. Please login again."
        		);
        }
        String email  = jwtUtil.extractEmail(token);
        String role   = jwtUtil.extractRole(token);
        int userId    = jwtUtil.extractUserId(token);
        return jwtUtil.generateToken(email, role, userId);
    }

    // getUserByEmail()
    @Override
    public User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
        		.orElseThrow(() -> new ResourceNotFoundException(
        			    USER_ENTITY, EMAIL_FIELD, email
        			));
    }

    //  getUserById()
    @Override
    public User getUserById(int userId) {
        return userRepository.findByUserId(userId)
        		.orElseThrow(() -> new ResourceNotFoundException(
                	    USER_ENTITY, "id", userId
                		));   
    }

    // updateProfile()
    @Override
    public User updateProfile(int userId, User updatedUser) {
        User existing = getUserById(userId);
        existing.setFullName(updatedUser.getFullName());
        existing.setPhone(updatedUser.getPhone());
        existing.setProfilePicUrl(updatedUser.getProfilePicUrl());
        return userRepository.save(existing);
    }

    @Override
    public void changePassword(int userId, String newPassword) {
        
        // find user â€” throws ResourceNotFoundException if not found
        User user = getUserById(userId);
        
        // validate new password is not empty
        if (newPassword == null || newPassword.trim().isEmpty()) {
            throw new BadRequestException(
                "New password cannot be empty."
            );
        }
        
        // validate minimum password length
        if (newPassword.length() < 6) {
            throw new BadRequestException(
                "Password must be at least 6 characters long."
            );
        }
        
        // encode new password with BCrypt
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        
        
        // save updated user
        userRepository.save(user);
    }
    //  deactivateAccount()
    @Override
    public void deactivateAccount(int userId) {
        User user = getUserById(userId);
        user.setActive(false);
        userRepository.save(user);
    }

    // getAllUsers() â€” Admin: fetch every user in the system
    @Override
    public java.util.List<User> getAllUsers() {
        return userRepository.findAll();
    }

    // getUsersByRole() â€” Admin: filter users by role
    @Override
    public java.util.List<User> getUsersByRole(String role) {
        return userRepository.findAllByRole(role);
    }

    // reactivateAccount() â€” Admin: re-enable a deactivated user
    @Override
    public void reactivateAccount(int userId) {
        User user = getUserById(userId);
        user.setActive(true);
        userRepository.save(user);
    }
    
   

    // sendOtp() â€” delegates to OtpService
    @Override
    public void sendOtp(String email) {
        otpService.generateAndSendOtp(email);
    }

    // verifyOtp() â€” delegates to OtpService
    @Override
    public boolean verifyOtp(String email, String otp) {
        return otpService.verifyOtp(email, otp);
    }
    
 // forgotPassword() â€” generate token + OTP, send email, print in console
    @Override
    public void forgotPassword(String email) {

        // Check user exists
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException(
                        USER_ENTITY, EMAIL_FIELD, email));

        // Delete any existing reset tokens for this email
        passwordResetTokenRepository.deleteAllByEmail(email);

        // Generate unique reset token
        String token = UUID.randomUUID().toString();

        // Generate 6-digit OTP
        String otp = String.format("%06d", new java.util.Random().nextInt(999999));

        // Save to DB
        PasswordResetToken resetToken = PasswordResetToken.builder()
                .email(email)
                .token(token)
                .otp(otp)
                .used(false)
                .build();
        passwordResetTokenRepository.save(resetToken);

        // Build reset link
        String resetLink = buildFrontendUrl("/reset-password") + "?token=" + token;

        // â”€â”€ Send email â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(email);
            message.setSubject("MediBook â€” Reset Your Password");
            message.setText(
                "Hello " + user.getFullName() + ",\n\n" +
                "You requested to reset your MediBook password.\n\n" +
                "Click the link below to reset your password:\n" +
                resetLink + "\n\n" +
                "Your OTP verification code: " + otp + "\n\n" +
                "This link and OTP are valid for 15 minutes only.\n" +
                "If you did not request this, please ignore this email.\n\n" +
                "â€” MediBook Team"
            );
            mailSender.send(message);
            System.out.println("[AuthService] Reset email sent to: " + email);
        } catch (Exception e) {
            System.err.println("[AuthService] Email sending failed: " + e.getMessage());
        }

        // â”€â”€ Print in console for testing â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€
        System.out.println("========================================");
        System.out.println("  MediBook Password Reset");
        System.out.println("  Email : " + email);
        System.out.println("  Token : " + token);
        System.out.println("  OTP   : " + otp);
        System.out.println("  Link  : " + resetLink);
        System.out.println("  Valid for 15 minutes");
        System.out.println("========================================");
    }

    // verifyResetOtp() â€” validate token + OTP before allowing password reset
    @Override
    public void verifyResetOtp(String token, String otp) {

        PasswordResetToken resetToken = passwordResetTokenRepository
                .findByToken(token)
                .orElseThrow(() -> new BadRequestException(
                        "Invalid or expired reset link. Please request a new one."));

        // Check already used
        if (resetToken.isUsed()) {
            throw new BadRequestException(
                    "This reset link has already been used. Please request a new one.");
        }

        // Check expired
        if (resetToken.getExpiresAt().isBefore(java.time.LocalDateTime.now())) {
            passwordResetTokenRepository.delete(resetToken);
            throw new BadRequestException(
                    "Reset link has expired. Please request a new one.");
        }

        // Check OTP matches
        if (!resetToken.getOtp().equals(otp)) {
            throw new BadRequestException("Invalid OTP. Please try again.");
        }

        // OTP verified â€” mark token as verified (not used yet, used after password set)
        resetToken.setUsed(false);
        passwordResetTokenRepository.save(resetToken);
    }

    // resetPassword() â€” save new password after OTP verified
    @Override
    public void resetPassword(String token, String newPassword) {

        PasswordResetToken resetToken = passwordResetTokenRepository
                .findByToken(token)
                .orElseThrow(() -> new BadRequestException(
                        "Invalid or expired reset link. Please request a new one."));

        // Check expired
        if (resetToken.getExpiresAt().isBefore(java.time.LocalDateTime.now())) {
            passwordResetTokenRepository.delete(resetToken);
            throw new BadRequestException(
                    "Reset link has expired. Please request a new one.");
        }

        // Validate new password
        if (newPassword == null || newPassword.trim().isEmpty()) {
            throw new BadRequestException("Password cannot be empty.");
        }
        if (newPassword.length() < 6) {
            throw new BadRequestException(
                    "Password must be at least 6 characters long.");
        }

        // Find user and update password
        User user = userRepository.findByEmail(resetToken.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException(
                        USER_ENTITY, EMAIL_FIELD, resetToken.getEmail()));

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        // Mark token as used and delete
        passwordResetTokenRepository.delete(resetToken);

        System.out.println("[AuthService] Password reset successful for: " + resetToken.getEmail());
    }

    private String buildFrontendUrl(String path) {
        return normalizeBaseUrl(frontendBaseUrl) + path;
    }

    private String normalizeBaseUrl(String baseUrl) {
        if (baseUrl == null || baseUrl.isBlank()) {
            return "http://localhost:5173";
        }
        return baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }
}
