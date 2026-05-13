package com.teampulse.backend.common.config;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = "app.cors.allowed-origins=https://teampulse-frontend-ruddy.vercel.app")
@AutoConfigureMockMvc
@ActiveProfiles("demo")
class SecurityConfigCorsTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void publicFrontendOriginsStayAllowedWhenEnvironmentOverridesCorsList() throws Exception {
        assertCorsPreflightAllowed("https://team-pulse-frontend.vercel.app");
        assertCorsPreflightAllowed("https://teampulse.com");
        assertCorsPreflightAllowed("https://www.teampulse.com");
    }

    private void assertCorsPreflightAllowed(String origin) throws Exception {
        mockMvc.perform(options("/api/projects/1/invitations")
                        .header("Origin", origin)
                        .header("Access-Control-Request-Method", "POST")
                        .header("Access-Control-Request-Headers", "authorization,content-type"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", origin))
                .andExpect(header().string("Access-Control-Allow-Credentials", "true"));
    }
}
