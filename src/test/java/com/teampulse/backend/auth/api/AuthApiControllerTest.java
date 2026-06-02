package com.teampulse.backend.auth.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("demo")
class AuthApiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void signupUsesEmailAndReturnsSpecResponse() throws Exception {
        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(signupBody("signup-success@example.com", "!aaa123123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.responseCode").value(1000))
                .andExpect(jsonPath("$.result.userId").isNumber())
                .andExpect(jsonPath("$.result.email").value("signup-success@example.com"))
                .andExpect(jsonPath("$.result.name").value("Test User"))
                .andExpect(jsonPath("$.result.university").value("Konkuk University"))
                .andExpect(jsonPath("$.result.phone").value("010-1234-1234"))
                .andExpect(jsonPath("$.result.jwtInfo.accessToken").exists())
                .andExpect(jsonPath("$.result.jwtInfo.refreshToken").exists());
    }

    @Test
    void signupRejectsDuplicateEmail() throws Exception {
        var body = signupBody("duplicate@example.com", "!aaa123123");

        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.responseCode").value(2012));
    }

    @Test
    void signupRejectsInvalidFields() throws Exception {
        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "not-email",
                                  "password": "aaa123123",
                                  "name": "",
                                  "university": "",
                                  "phone": "010-1234-1234"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.responseCode").value(2020))
                .andExpect(jsonPath("$.result.errors").isArray());
    }

    @Test
    void loginUsesEmailAndReturnsSpecResponse() throws Exception {
        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(signupBody("login-success@example.com", "!aaa123123")))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "LOGIN-SUCCESS@example.com",
                                  "password": "!aaa123123"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.responseCode").value(1000))
                .andExpect(jsonPath("$.result.userId").isNumber())
                .andExpect(jsonPath("$.result.email").value("login-success@example.com"))
                .andExpect(jsonPath("$.result.jwtInfo.accessToken").exists())
                .andExpect(jsonPath("$.result.jwtInfo.refreshToken").exists());
    }

    @Test
    void loginRejectsUnknownEmail() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "unknown-login@example.com",
                                  "password": "!aaa123123"
                                }
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.responseCode").value(2013));
    }

    @Test
    void loginRejectsWrongPassword() throws Exception {
        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(signupBody("wrong-password@example.com", "!aaa123123")))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "wrong-password@example.com",
                                  "password": "!bbb123123"
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.responseCode").value(2014));
    }

    @Test
    void loginRateLimitBlocksRepeatedFailures() throws Exception {
        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(signupBody("rate-limit@example.com", "!aaa123123")))
                .andExpect(status().isOk());

        for (int attempt = 0; attempt < 5; attempt++) {
            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "email": "rate-limit@example.com",
                                      "password": "!wrong123123"
                                    }
                                    """))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.responseCode").value(2014));
        }

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "rate-limit@example.com",
                                  "password": "!aaa123123"
                                }
                                """))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.responseCode").value(2015));
    }

    @Test
    void loginRejectsInvalidFields() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "not-email",
                                  "password": "aaa123123"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.responseCode").value(2020))
                .andExpect(jsonPath("$.result.errors").isArray());
    }

    @Test
    void logoutRevokesRefreshToken() throws Exception {
        var signupResult = mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(signupBody("logout-success@example.com", "!aaa123123")))
                .andExpect(status().isOk())
                .andReturn();

        String refreshToken = JsonPath.read(
                signupResult.getResponse().getContentAsString(),
                "$.result.jwtInfo.refreshToken");

        mockMvc.perform(post("/api/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(logoutBody(refreshToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.responseCode").value(1000))
                .andExpect(jsonPath("$.result").doesNotExist());

        mockMvc.perform(post("/api/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(logoutBody(refreshToken)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.responseCode").value(2009));
    }

    @Test
    void logoutRejectsInvalidRefreshToken() throws Exception {
        mockMvc.perform(post("/api/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(logoutBody("Bearer invalid-refresh-token")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.responseCode").value(2009));
    }

    @Test
    void logoutRejectsMissingRefreshToken() throws Exception {
        mockMvc.perform(post("/api/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.responseCode").value(2020))
                .andExpect(jsonPath("$.result.errors").isArray());
    }

    private String signupBody(String email, String password) {
        return """
                {
                  "email": "%s",
                  "password": "%s",
                  "name": "Test User",
                  "university": "Konkuk University",
                  "phone": "010-1234-1234"
                }
                """.formatted(email, password);
    }

    private String logoutBody(String refreshToken) {
        return """
                {
                  "refreshToken": "%s"
                }
                """.formatted(refreshToken);
    }
}
