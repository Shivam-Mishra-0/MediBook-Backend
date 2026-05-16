package com.medibook.auth.security;

import com.medibook.auth.entity.User;
import com.medibook.auth.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
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
    private String frontendBaseUrl = "http://localhost:5173";

    public OAuth2SuccessHandler(UserRepository userRepository, JwtUtil jwtUtil) {
        this.userRepository = userRepository;
        this.jwtUtil = jwtUtil;
    }

    @Value("${app.frontend.base-url:http://localhost:5173}")
    public void setFrontendBaseUrl(String frontendBaseUrl) {
        this.frontendBaseUrl = normalizeBaseUrl(frontendBaseUrl);
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {

        OAuth2User oauthUser = (OAuth2User) authentication.getPrincipal();
        String email = oauthUser.getAttribute("email");
        String name = oauthUser.getAttribute("name");
        String picture = oauthUser.getAttribute("picture");

        User existingUser = userRepository.findByEmail(email).orElse(null);

        if (existingUser != null) {
            if (!existingUser.isVerified()) {
                existingUser.setVerified(true);
                userRepository.save(existingUser);
            }

            String token = jwtUtil.generateToken(
                    existingUser.getEmail(),
                    existingUser.getRole(),
                    existingUser.getUserId()
            );
            String redirectUrl = buildFrontendUrl("/oauth2/callback")
                    + "?token=" + URLEncoder.encode(token, StandardCharsets.UTF_8)
                    + "&userId=" + existingUser.getUserId()
                    + "&role=" + URLEncoder.encode(existingUser.getRole(), StandardCharsets.UTF_8)
                    + "&fullName=" + URLEncoder.encode(existingUser.getFullName(), StandardCharsets.UTF_8)
                    + "&email=" + URLEncoder.encode(existingUser.getEmail(), StandardCharsets.UTF_8);

            getRedirectStrategy().sendRedirect(request, response, redirectUrl);
            return;
        }

        String redirectUrl = buildFrontendUrl("/oauth2/select-role")
                + "?email=" + URLEncoder.encode(email, StandardCharsets.UTF_8)
                + "&name=" + URLEncoder.encode(name, StandardCharsets.UTF_8)
                + "&picture=" + URLEncoder.encode(picture != null ? picture : "", StandardCharsets.UTF_8)
                + "&provider=google";

        getRedirectStrategy().sendRedirect(request, response, redirectUrl);
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
