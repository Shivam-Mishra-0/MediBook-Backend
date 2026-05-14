package com.medibook.auth.security;

import com.medibook.auth.entity.User;
import com.medibook.auth.repository.UserRepository;
import com.medibook.otp.service.OtpService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Component
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;
    private final OtpService otpService;

    public OAuth2SuccessHandler(UserRepository userRepository, JwtUtil jwtUtil, OtpService otpService) {
        this.userRepository = userRepository;
        this.jwtUtil = jwtUtil;
        this.otpService = otpService;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {

        OAuth2User oauthUser = (OAuth2User) authentication.getPrincipal();
        String email   = oauthUser.getAttribute("email");
        String name    = oauthUser.getAttribute("name");
        String picture = oauthUser.getAttribute("picture");

        User existingUser = userRepository.findByEmail(email).orElse(null);

        if (existingUser != null) {

            // verified Google users go straight to dashboard via token
            if (existingUser.isVerified())  {
                String token = jwtUtil.generateToken(
                    existingUser.getEmail(),
                    existingUser.getRole(),
                    existingUser.getUserId()
                );
                String redirectUrl = "http://localhost:5173/oauth2/callback"
                        + "?token="     + URLEncoder.encode(token, StandardCharsets.UTF_8)
                        + "&userId="    + existingUser.getUserId()
                        + "&role="      + URLEncoder.encode(existingUser.getRole(), StandardCharsets.UTF_8)
                        + "&fullName="  + URLEncoder.encode(existingUser.getFullName(), StandardCharsets.UTF_8);

                getRedirectStrategy().sendRedirect(request, response, redirectUrl);
                return;
            }

            // First Google login - still needs OTP.
            // Email OTP only for Google users (no phone required).
            otpService.generateAndSendOtp(email);

            String redirectUrl = "http://localhost:5173/otp"
                    + "?email=" + URLEncoder.encode(email, StandardCharsets.UTF_8)
                    + "&name="  + URLEncoder.encode(existingUser.getFullName(), StandardCharsets.UTF_8)
                    + "&source=google";

            getRedirectStrategy().sendRedirect(request, response, redirectUrl);

        } else {
            // ── NEW GOOGLE USER → select role first, OTP after role picked ──
            String redirectUrl = "http://localhost:5173/oauth2/select-role"
                    + "?email="   + URLEncoder.encode(email, StandardCharsets.UTF_8)
                    + "&name="    + URLEncoder.encode(name, StandardCharsets.UTF_8)
                    + "&picture=" + URLEncoder.encode(picture != null ? picture : "", StandardCharsets.UTF_8)
                    + "&provider=google";

            getRedirectStrategy().sendRedirect(request, response, redirectUrl);
        }
    }
}
