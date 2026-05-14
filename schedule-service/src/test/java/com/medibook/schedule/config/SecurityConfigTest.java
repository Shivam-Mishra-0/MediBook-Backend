package com.medibook.schedule.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@WebMvcTest(controllers = SecurityProbeController.class)
@Import(SecurityConfig.class)
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SecurityFilterChain securityFilterChain;

    @Test
    void filterChainBeanIsCreated() {
        assertThat(securityFilterChain).isNotNull();
    }

    @Test
    void permitsAnonymousGetRequests() throws Exception {
        mockMvc.perform(get("/security-probe"))
                .andExpect(status().isOk())
                .andExpect(content().string("ok"));
    }

    @Test
    void disablesCsrfForPostRequests() throws Exception {
        mockMvc.perform(post("/security-probe"))
                .andExpect(status().isOk())
                .andExpect(content().string("posted"));
    }
}

@RestController
class SecurityProbeController {

    @GetMapping("/security-probe")
    public String getProbe() {
        return "ok";
    }

    @PostMapping("/security-probe")
    public String postProbe() {
        return "posted";
    }
}
