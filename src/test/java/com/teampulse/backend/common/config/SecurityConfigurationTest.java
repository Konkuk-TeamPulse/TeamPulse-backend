package com.teampulse.backend.common.config;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("demo")
@TestPropertySource(properties = "app.security.public-legacy-mobile-api=false")
class SecurityConfigurationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void legacyMobileApisCanBeProtectedWithoutChangingPaths() throws Exception {
        mockMvc.perform(get("/api/mobile/workspace"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.responseCode").value(3001))
                .andExpect(jsonPath("$.responseMessage").value("로그인이 필요합니다."));
    }
}
