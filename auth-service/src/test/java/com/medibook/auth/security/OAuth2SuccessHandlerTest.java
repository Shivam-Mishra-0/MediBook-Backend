package com.medibook.auth.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;

import com.medibook.auth.entity.User;
import com.medibook.auth.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class OAuth2SuccessHandlerTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private JwtUtil jwtUtil;

    @InjectMocks
    private OAuth2SuccessHandler successHandler;

    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    @BeforeEach
    void setUp() {
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
    }

    @Test
    @DisplayName("verified Google users are redirected with a JWT")
    void existingVerifiedUserRedirectsToCallback() throws IOException {
        User existingUser = User.builder()
                .userId(9)
                .email("verified@test.com")
                .fullName("Verified User")
                .role("Patient")
                .verified(true)
                .isActive(true)
                .build();

        when(userRepository.findByEmail("verified@test.com")).thenReturn(Optional.of(existingUser));
        when(jwtUtil.generateToken("verified@test.com", "Patient", 9)).thenReturn("jwt-value");

        successHandler.onAuthenticationSuccess(
                request,
                response,
                authentication("verified@test.com", "Verified User", "http://pic")
        );

        assertThat(response.getRedirectedUrl()).contains("/oauth2/callback");
        assertThat(response.getRedirectedUrl()).contains("token=jwt-value");
        assertThat(response.getRedirectedUrl()).contains("userId=9");
        assertThat(response.getRedirectedUrl()).contains("role=Patient");
    }

    @Test
    @DisplayName("existing unverified Google users are verified and redirected with a JWT")
    void existingUnverifiedUserRedirectsToCallback() throws IOException {
        User existingUser = User.builder()
                .userId(10)
                .email("otp@test.com")
                .fullName("OTP User")
                .role("Provider")
                .verified(false)
                .isActive(true)
                .build();

        when(userRepository.findByEmail("otp@test.com")).thenReturn(Optional.of(existingUser));
        when(userRepository.save(existingUser)).thenReturn(existingUser);
        when(jwtUtil.generateToken("otp@test.com", "Provider", 10)).thenReturn("jwt-for-google-user");

        successHandler.onAuthenticationSuccess(
                request,
                response,
                authentication("otp@test.com", "OTP User", "http://pic")
        );

        verify(userRepository).save(existingUser);
        assertThat(existingUser.isVerified()).isTrue();
        assertThat(response.getRedirectedUrl()).contains("/oauth2/callback");
        assertThat(response.getRedirectedUrl()).contains("token=jwt-for-google-user");
        assertThat(response.getRedirectedUrl()).contains("email=otp%40test.com");
    }

    @Test
    @DisplayName("new Google users are redirected to the role-selection page")
    void newUserRedirectsToRoleSelection() throws IOException {
        when(userRepository.findByEmail("new@test.com")).thenReturn(Optional.empty());

        successHandler.onAuthenticationSuccess(
                request,
                response,
                authentication("new@test.com", "New User", null)
        );

        assertThat(response.getRedirectedUrl()).contains("/oauth2/select-role");
        assertThat(response.getRedirectedUrl()).contains("email=new%40test.com");
        assertThat(response.getRedirectedUrl()).contains("provider=google");
        assertThat(response.getRedirectedUrl()).contains("picture=");
    }

    private UsernamePasswordAuthenticationToken authentication(String email, String name, String picture) {
        Map<String, Object> attributes = new HashMap<>();
        attributes.put("email", email);
        attributes.put("name", name);
        attributes.put("picture", picture);

        OAuth2User oauth2User = new DefaultOAuth2User(
                List.of(new SimpleGrantedAuthority("ROLE_USER")),
                attributes,
                "email"
        );

        return new UsernamePasswordAuthenticationToken(oauth2User, null, oauth2User.getAuthorities());
    }
}
