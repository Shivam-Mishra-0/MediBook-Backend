package com.medibook.admin.config;

import com.medibook.admin.resource.AdminResource;
import com.medibook.admin.security.JwtFilter;
import com.medibook.admin.security.JwtUtil;
import com.medibook.admin.service.AdminService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AdminResource.class)
@Import({SecurityConfig.class, JwtFilter.class})
@DisplayName("SecurityConfig Tests")
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AdminService adminService;

    @MockBean
    private JwtUtil jwtUtil;

    @Test
    @DisplayName("Admin endpoints are forbidden without authentication")
    void adminEndpointsRequireAuthentication() throws Exception {
        mockMvc.perform(get("/admin/ping"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(authorities = "Admin")
    @DisplayName("Admin authority can access admin endpoints")
    void adminAuthorityCanAccessAdminEndpoints() throws Exception {
        mockMvc.perform(get("/admin/ping"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(authorities = "Patient")
    @DisplayName("Non-admin authority is forbidden from admin endpoints")
    void nonAdminAuthorityIsForbidden() throws Exception {
        mockMvc.perform(get("/admin/ping"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Public actuator endpoints are not blocked by security")
    void actuatorEndpointsArePublic() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isInternalServerError());
    }

    @Test
    @DisplayName("passwordEncoder bean creates a working BCrypt encoder")
    void passwordEncoderBeanWorks() {
        PasswordEncoder passwordEncoder = new SecurityConfig().passwordEncoder();
        String encoded = passwordEncoder.encode("secret123");

        assertThat(encoded).isNotEqualTo("secret123");
        assertThat(passwordEncoder.matches("secret123", encoded)).isTrue();
    }

    @Test
    @DisplayName("authenticationManager bean delegates to AuthenticationConfiguration")
    void authenticationManagerDelegatesToConfiguration() throws Exception {
        SecurityConfig securityConfig = new SecurityConfig();
        AuthenticationConfiguration configuration = Mockito.mock(AuthenticationConfiguration.class);
        AuthenticationManager authenticationManager = Mockito.mock(AuthenticationManager.class);
        when(configuration.getAuthenticationManager()).thenReturn(authenticationManager);

        assertThat(securityConfig.authenticationManager(configuration)).isSameAs(authenticationManager);
    }
}
