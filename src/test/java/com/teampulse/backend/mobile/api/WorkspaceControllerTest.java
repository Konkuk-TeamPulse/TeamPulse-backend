package com.teampulse.backend.mobile.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
class WorkspaceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void mobileWorkspaceEndpointIsAccessibleWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/api/mobile/workspace"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void bootstrapRejectsInvalidDueDate() throws Exception {
        mockMvc.perform(post("/api/mobile/workspace/bootstrap")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Kim",
                                  "email": "kim@example.com",
                                  "teamName": "TeamPulse",
                                  "courseName": "AI Coding Tool",
                                  "semester": "2026-1",
                                  "dueDate": "2026-13-40"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void createTaskRejectsUnknownOwner() throws Exception {
        mockMvc.perform(post("/api/mobile/workspace/bootstrap")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Kim",
                                  "email": "kim@example.com",
                                  "teamName": "TeamPulse",
                                  "courseName": "AI Coding Tool",
                                  "semester": "2026-1",
                                  "dueDate": "2026-04-12"
                                }
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/mobile/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Build backend",
                                  "owner": "Min",
                                  "dueDate": "2026-04-10",
                                  "blockers": []
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("INVALID_INPUT"));
    }
}
