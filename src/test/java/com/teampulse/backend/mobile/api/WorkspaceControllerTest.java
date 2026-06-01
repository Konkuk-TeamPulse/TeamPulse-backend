package com.teampulse.backend.mobile.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.parser.PdfTextExtractor;
import com.teampulse.backend.mobile.application.WorkspaceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("demo")
class WorkspaceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private WorkspaceService workspaceService;

    @BeforeEach
    void resetWorkspace() {
        workspaceService.reset();
    }

    @Test
    void accountActivitiesExposeApiSpecRefinements() throws Exception {
        String accessToken = issueAccessToken("account-activities@example.com", "Lee Juho");

        mockMvc.perform(post("/api/projects")
                        .header("Authorization", accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "projectName": "TeamPulse",
                                  "subject": "AI Coding Tool",
                                  "description": "Risk API test project",
                                  "startDate": "2026-04-01",
                                  "endDate": "2026-06-09"
                                }
                                """))
                .andExpect(status().isOk());

        MvcResult membersResult = mockMvc.perform(get("/api/projects/1/members")
                        .header("Authorization", accessToken))
                .andExpect(status().isOk())
                .andReturn();
        Number assigneeId = JsonPath.read(membersResult.getResponse().getContentAsString(), "$.result[0].memberId");

        mockMvc.perform(post("/api/projects/1/tasks")
                        .header("Authorization", accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Overdue risk task",
                                  "description": "Creates a risk signal",
                                  "assigneeId": %d,
                                  "dueDate": "2026-04-01"
                                }
                                """.formatted(assigneeId.longValue())))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/projects/1/activity-logs")
                        .header("Authorization", accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.result[0].userName").value("Lee Juho"))
                .andExpect(jsonPath("$.result[0].updatedAt").exists());

        mockMvc.perform(get("/api/projects/1/risks")
                        .header("Authorization", accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].title").value("진행 정체"))
                .andExpect(jsonPath("$.data[0].affectedTaskIds").isArray())
                .andExpect(jsonPath("$.data[0].suggestedActions").isArray());

    }

    @Test
    void projectApisUseNotionSpecRequestAndResponseShape() throws Exception {
        mockMvc.perform(get("/api/projects"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.responseCode").value(3001))
                .andExpect(jsonPath("$.responseMessage").value("로그인이 필요합니다."));

        String accessToken = issueAccessToken("project-list@example.com");

        mockMvc.perform(get("/api/projects")
                        .header("Authorization", accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.responseCode").value(1000))
                .andExpect(jsonPath("$.result").isArray())
                .andExpect(jsonPath("$.result.length()").value(0));

        mockMvc.perform(post("/api/projects")
                        .header("Authorization", accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "projectName": "TeamPulse Project",
                                  "subject": "Advanced Project",
                                  "description": "TeamPulse backend implementation",
                                  "startDate": "2026-04-01",
                                  "endDate": "2026-06-09"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.responseCode").value(1000))
                .andExpect(jsonPath("$.result.projectId").value(1))
                .andExpect(jsonPath("$.result.projectName").value("TeamPulse Project"))
                .andExpect(jsonPath("$.result.role").value("LEADER"));

        mockMvc.perform(get("/api/projects")
                        .header("Authorization", accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.result[0].projectId").value(1))
                .andExpect(jsonPath("$.result[0].projectName").value("TeamPulse Project"))
                .andExpect(jsonPath("$.result[0].subject").value("Advanced Project"))
                .andExpect(jsonPath("$.result[0].role").value("LEADER"))
                .andExpect(jsonPath("$.result[0].endDate").value("2026-06-09"));

        mockMvc.perform(get("/api/projects/1")
                        .header("Authorization", accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.result.projectId").value(1))
                .andExpect(jsonPath("$.result.projectName").value("TeamPulse Project"))
                .andExpect(jsonPath("$.result.subject").value("Advanced Project"))
                .andExpect(jsonPath("$.result.description").value("TeamPulse backend implementation"))
                .andExpect(jsonPath("$.result.startDate").value("2026-04-01"))
                .andExpect(jsonPath("$.result.endDate").value("2026-06-09"))
                .andExpect(jsonPath("$.result.memberCount").value(1));

        mockMvc.perform(get("/api/projects/999")
                        .header("Authorization", accessToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.responseCode").value(4001))
                .andExpect(jsonPath("$.responseMessage").value("프로젝트를 찾을 수 없습니다."));

        mockMvc.perform(patch("/api/projects/1")
                        .header("Authorization", accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "projectName": "TeamPulse Updated",
                                  "subject": "Capstone",
                                  "description": "Updated project description",
                                  "startDate": "2026-04-02",
                                  "endDate": "2026-06-10"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.responseCode").value(1000))
                .andExpect(jsonPath("$.result.projectId").value(1))
                .andExpect(jsonPath("$.result.projectName").value("TeamPulse Updated"))
                .andExpect(jsonPath("$.result.subject").value("Capstone"))
                .andExpect(jsonPath("$.result.description").value("Updated project description"))
                .andExpect(jsonPath("$.result.startDate").value("2026-04-02"))
                .andExpect(jsonPath("$.result.endDate").value("2026-06-10"))
                .andExpect(jsonPath("$.result.updatedAt").exists());
    }

    @Test
    void accountDashboardActivityMembersAndLeaveUseSpecResponseShape() throws Exception {
        String accessToken = issueAccessToken("me-lookup@example.com", "Account Owner");

        mockMvc.perform(post("/api/projects")
                        .header("Authorization", accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "projectName": "Progressing API Project",
                                  "subject": "Advanced Project",
                                  "description": "Progressing API test project",
                                  "startDate": "2026-04-01",
                                  "endDate": "2026-06-09"
                                }
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.responseCode").value(3001));

        mockMvc.perform(get("/api/users/me")
                        .header("Authorization", accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.responseCode").value(1000))
                .andExpect(jsonPath("$.result.userId").isNumber())
                .andExpect(jsonPath("$.result.email").value("me-lookup@example.com"))
                .andExpect(jsonPath("$.result.name").value("Account Owner"))
                .andExpect(jsonPath("$.result.university").value("Konkuk University"))
                .andExpect(jsonPath("$.result.phone").value("010-1234-5678"));

        mockMvc.perform(get("/api/projects/1/dashboard")
                        .header("Authorization", accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.responseCode").value(1000))
                .andExpect(jsonPath("$.result.projectId").value(1))
                .andExpect(jsonPath("$.result.projectName").value("Progressing API Project"))
                .andExpect(jsonPath("$.result.taskSummary.totalTaskCount").value(0))
                .andExpect(jsonPath("$.result.scheduleSummary.projectStartDate").value("2026-04-01"))
                .andExpect(jsonPath("$.result.memberWorkload[0].name").value("Account Owner"))
                .andExpect(jsonPath("$.result.riskSummary.totalRiskCount").isNumber());

        mockMvc.perform(get("/api/projects/1/activity-logs")
                        .header("Authorization", accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.responseCode").value(1000))
                .andExpect(jsonPath("$.result[0].logId").isNumber())
                .andExpect(jsonPath("$.result[0].content").exists())
                .andExpect(jsonPath("$.result[0].userName").value("Account Owner"))
                .andExpect(jsonPath("$.result[0].updatedAt").exists());

        mockMvc.perform(get("/api/projects/1/members")
                        .header("Authorization", accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.responseCode").value(1000))
                .andExpect(jsonPath("$.result[0].memberId").isNumber())
                .andExpect(jsonPath("$.result[0].email").value("me-lookup@example.com"))
                .andExpect(jsonPath("$.result[0].role").value("LEADER"));

        mockMvc.perform(delete("/api/projects/1/members/me")
                        .header("Authorization", accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.responseCode").value(1000))
                .andExpect(jsonPath("$.result").doesNotExist());
    }

    @Test
    void taskApisUseNotionSpecRequestAndResponseShape() throws Exception {
        String accessToken = issueAccessToken("task-owner@example.com", "Task Owner");

        mockMvc.perform(post("/api/projects")
                        .header("Authorization", accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "projectName": "Task Project",
                                  "subject": "Advanced Project",
                                  "description": "Task API test project",
                                  "startDate": "2026-04-01",
                                  "endDate": "2026-06-09"
                                }
                                """))
                .andExpect(status().isOk());

        MvcResult membersResult = mockMvc.perform(get("/api/projects/1/members")
                        .header("Authorization", accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andReturn();
        Number assigneeId = JsonPath.read(membersResult.getResponse().getContentAsString(), "$.result[0].memberId");

        mockMvc.perform(post("/api/projects/1/tasks")
                        .header("Authorization", accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Invalid assignee task",
                                  "description": "Should fail with typed error",
                                  "assigneeId": 999999,
                                  "dueDate": "2026-04-28"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.responseCode").value(4002))
                .andExpect(jsonPath("$.responseMessage").value("팀원을 찾을 수 없습니다."));

        MvcResult createResult = mockMvc.perform(post("/api/projects/1/tasks")
                        .header("Authorization", accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Write API spec",
                                  "description": "Draft task API spec",
                                  "assigneeId": %d,
                                  "dueDate": "2026-04-28"
                                }
                                """.formatted(assigneeId.longValue())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.responseCode").value(1000))
                .andExpect(jsonPath("$.result.title").value("Write API spec"))
                .andExpect(jsonPath("$.result.status").value("TODO"))
                .andExpect(jsonPath("$.result.assigneeId").value(assigneeId.longValue()))
                .andExpect(jsonPath("$.result.dueDate").value("2026-04-28"))
                .andReturn();
        Number taskId = JsonPath.read(createResult.getResponse().getContentAsString(), "$.result.taskId");

        MvcResult precedingResult = mockMvc.perform(post("/api/projects/1/tasks")
                        .header("Authorization", accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Prepare API data",
                                  "description": "Prepare task dependency source",
                                  "assigneeId": %d,
                                  "dueDate": "2026-04-27"
                                }
                                """.formatted(assigneeId.longValue())))
                .andExpect(status().isOk())
                .andReturn();
        Number precedingTaskId = JsonPath.read(precedingResult.getResponse().getContentAsString(), "$.result.taskId");

        mockMvc.perform(post("/api/tasks/{taskId}/dependencies", taskId.longValue())
                        .header("Authorization", accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "precedingTaskId": %d
                                }
                                """.formatted(precedingTaskId.longValue())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.responseCode").value(1000))
                .andExpect(jsonPath("$.result.taskId").value(taskId.longValue()))
                .andExpect(jsonPath("$.result.precedingTaskId").value(precedingTaskId.longValue()))
                .andExpect(jsonPath("$.result.taskTitle").value("Write API spec"))
                .andExpect(jsonPath("$.result.precedingTaskTitle").value("Prepare API data"));

        mockMvc.perform(get("/api/projects/1/tasks")
                        .header("Authorization", accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.result[?(@.taskId == %d)].precedingTaskIds[0]".formatted(taskId.longValue()))
                        .value(hasItem(precedingTaskId.intValue())))
                .andExpect(jsonPath("$.result[?(@.taskId == %d)].blockedTaskIds[0]".formatted(precedingTaskId.longValue()))
                        .value(hasItem(taskId.intValue())));

        mockMvc.perform(get("/api/projects/1/dashboard")
                        .header("Authorization", accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.result.risks[0].type").value("PROGRESS_STALLED"))
                .andExpect(jsonPath("$.result.risks[0].relatedTaskId").value(precedingTaskId.longValue()))
                .andExpect(jsonPath("$.result.risks[0].relatedMemberName").value("Task Owner"));

        mockMvc.perform(post("/api/tasks/{taskId}/dependencies", precedingTaskId.longValue())
                        .header("Authorization", accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "precedingTaskId": %d
                                }
                                """.formatted(taskId.longValue())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.responseCode").value(3006));

        mockMvc.perform(post("/api/tasks/{taskId}/dependencies", taskId.longValue())
                        .header("Authorization", accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "precedingTaskId": %d
                                }
                                """.formatted(taskId.longValue())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.responseCode").value(3006))
                .andExpect(jsonPath("$.responseMessage").value("자기 자신을 선행 태스크로 설정할 수 없습니다."));

        mockMvc.perform(delete("/api/tasks/{taskId}/dependencies/{dependencyId}", taskId.longValue(), precedingTaskId.longValue())
                        .header("Authorization", accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.responseCode").value(1000))
                .andExpect(jsonPath("$.result").doesNotExist());

        mockMvc.perform(get("/api/projects/1/tasks")
                        .header("Authorization", accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.responseCode").value(1000))
                .andExpect(jsonPath("$.result[?(@.taskId == %d)].title".formatted(taskId.longValue())).value(hasItem("Write API spec")))
                .andExpect(jsonPath("$.result[?(@.taskId == %d)].assigneeName".formatted(taskId.longValue())).value(hasItem("Task Owner")))
                .andExpect(jsonPath("$.result[?(@.taskId == %d)].dueDate".formatted(taskId.longValue())).value(hasItem("2026-04-28")));

        mockMvc.perform(patch("/api/tasks/{taskId}", taskId.longValue())
                        .header("Authorization", accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Finalize API spec",
                                  "description": "Finalize task API spec",
                                  "assigneeId": %d,
                                  "dueDate": "2026-04-29"
                                }
                                """.formatted(assigneeId.longValue())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.responseCode").value(1000))
                .andExpect(jsonPath("$.result.taskId").value(taskId.longValue()))
                .andExpect(jsonPath("$.result.title").value("Finalize API spec"))
                .andExpect(jsonPath("$.result.description").value("Finalize task API spec"))
                .andExpect(jsonPath("$.result.dueDate").value("2026-04-29"));

        mockMvc.perform(patch("/api/tasks/{taskId}/status", taskId.longValue())
                        .header("Authorization", accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status": "DOING"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.responseCode").value(1000))
                .andExpect(jsonPath("$.result.taskId").value(taskId.longValue()))
                .andExpect(jsonPath("$.result.status").value("DOING"));

        mockMvc.perform(patch("/api/tasks/{taskId}/status", taskId.longValue())
                        .header("Authorization", accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status": "IN_PROGRESS"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.responseCode").value(2020));

        mockMvc.perform(delete("/api/tasks/{taskId}", taskId.longValue())
                        .header("Authorization", accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.responseCode").value(1000))
                .andExpect(jsonPath("$.result").doesNotExist());
    }

    @Test
    void meetingApisUseSpecResponseShape() throws Exception {
        String accessToken = issueAccessToken("meeting-detail@example.com", "Meeting Owner");

        mockMvc.perform(post("/api/projects")
                        .header("Authorization", accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "projectName": "Meeting Project",
                                  "subject": "Advanced Project",
                                  "description": "Meeting API test project",
                                  "startDate": "2026-04-01",
                                  "endDate": "2026-06-09"
                                }
                                """))
                .andExpect(status().isOk());

        MvcResult membersResult = mockMvc.perform(get("/api/projects/1/members")
                        .header("Authorization", accessToken))
                .andExpect(status().isOk())
                .andReturn();
        Number memberId = JsonPath.read(membersResult.getResponse().getContentAsString(), "$.result[0].memberId");

        MvcResult createResult = mockMvc.perform(post("/api/projects/1/meetings")
                        .header("Authorization", accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Weekly Sync",
                                  "meetingDate": "2026-04-29",
                                  "agenda": "Review backend progress",
                                  "content": "Discuss remaining in-progress APIs",
                                  "decisions": [
                                    "Keep Notion API shape",
                                    "Accept frontend array payload"
                                  ],
                                  "attendeeIds": [%d],
                                  "actionItems": [
                                    {
                                      "content": "Implement meeting APIs",
                                      "assigneeId": %d,
                                      "dueDate": "2026-04-30"
                                    }
                                  ]
                                }
                                """.formatted(memberId.longValue(), memberId.longValue())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.responseCode").value(1000))
                .andExpect(jsonPath("$.result.title").value("Weekly Sync"))
                .andExpect(jsonPath("$.result.meetingDate").value("2026-04-29"))
                .andReturn();
        Number meetingId = JsonPath.read(createResult.getResponse().getContentAsString(), "$.result.meetingId");

        mockMvc.perform(get("/api/projects/1/meetings")
                        .header("Authorization", accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.responseCode").value(1000))
                .andExpect(jsonPath("$.result[0].meetingId").value(meetingId.longValue()))
                .andExpect(jsonPath("$.result[0].title").value("Weekly Sync"))
                .andExpect(jsonPath("$.result[0].meetingDate").value("2026-04-29"))
                .andExpect(jsonPath("$.result[0].content").value("Discuss remaining in-progress APIs"))
                .andExpect(jsonPath("$.result[0].attendeeIds[0]").value(memberId.longValue()))
                .andExpect(jsonPath("$.result[0].actionItems[0].content").value("Implement meeting APIs"))
                .andExpect(jsonPath("$.result[0].actionItems[0].assigneeId").value(memberId.longValue()))
                .andExpect(jsonPath("$.result[0].actionItems[0].dueDate").value("2026-04-30"));

        mockMvc.perform(get("/api/meetings/{meetingId}", meetingId.longValue())
                        .header("Authorization", accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.responseCode").value(1000))
                .andExpect(jsonPath("$.result.meetingId").value(meetingId.longValue()))
                .andExpect(jsonPath("$.result.projectId").value(1))
                .andExpect(jsonPath("$.result.meetingDate").value("2026-04-29"))
                .andExpect(jsonPath("$.result.attendees[0].memberId").value(memberId.longValue()))
                .andExpect(jsonPath("$.result.attendees[0].name").value("Meeting Owner"))
                .andExpect(jsonPath("$.result.actionItems[0].assigneeMemberId").value(memberId.longValue()))
                .andExpect(jsonPath("$.result.actionItems[0].assigneeName").value("Meeting Owner"))
                .andExpect(jsonPath("$.result.createdAt").exists())
                .andExpect(jsonPath("$.result.updatedAt").exists());
    }

    @Test
    void invitationApisUseNotionPathsAndSpecResponseShape() throws Exception {
        String leaderToken = issueAccessToken("invitation-leader@example.com", "Invitation Leader");

        mockMvc.perform(post("/api/projects")
                        .header("Authorization", leaderToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "projectName": "Invitation Project",
                                  "subject": "Advanced Project",
                                  "description": "Invitation API test project",
                                  "startDate": "2026-04-01",
                                  "endDate": "2026-06-09"
                                }
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/projects/1/invitations"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.responseCode").value(3001));

        MvcResult invitationResult = mockMvc.perform(post("/api/projects/1/invitations")
                        .header("Authorization", leaderToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.responseCode").value(1000))
                .andExpect(jsonPath("$.result.projectId").value(1))
                .andExpect(jsonPath("$.result.inviteCode").isString())
                .andExpect(jsonPath("$.result.inviteUrl").isString())
                .andExpect(jsonPath("$.result.expiredAt").exists())
                .andReturn();
        String inviteCode = JsonPath.read(invitationResult.getResponse().getContentAsString(), "$.result.inviteCode");
        String inviteUrl = JsonPath.read(invitationResult.getResponse().getContentAsString(), "$.result.inviteUrl");
        assertThat(inviteUrl).isEqualTo("https://team-pulse-frontend.vercel.app/invite/" + inviteCode);

        mockMvc.perform(get("/api/invitations/{inviteCode}", inviteCode))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.responseCode").value(1000))
                .andExpect(jsonPath("$.result.projectId").value(1))
                .andExpect(jsonPath("$.result.projectName").value("Invitation Project"))
                .andExpect(jsonPath("$.result.subject").value("Advanced Project"))
                .andExpect(jsonPath("$.result.inviteCode").value(inviteCode));

        mockMvc.perform(post("/api/invitations/{inviteCode}/accept", inviteCode)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.responseCode").value(3001));

        String accessToken = issueAccessToken("invitation-accept@example.com");

        MvcResult acceptResult = mockMvc.perform(post("/api/invitations/{inviteCode}/accept", inviteCode)
                        .header("Authorization", accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "role": "LEADER"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.responseCode").value(1000))
                .andExpect(jsonPath("$.result.memberId").isNumber())
                .andExpect(jsonPath("$.result.projectId").value(1))
                .andExpect(jsonPath("$.result.projectName").value("Invitation Project"))
                .andExpect(jsonPath("$.result.role").value("MEMBER"))
                .andReturn();
        Number invitedMemberId = JsonPath.read(acceptResult.getResponse().getContentAsString(), "$.result.memberId");

        mockMvc.perform(get("/api/projects/1/members")
                        .header("Authorization", leaderToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.length()").value(2));

        mockMvc.perform(delete("/api/projects/1/members/{memberId}", invitedMemberId.longValue()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.responseCode").value(3001));

        mockMvc.perform(delete("/api/projects/1/members/{memberId}", invitedMemberId.longValue())
                        .header("Authorization", accessToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.responseCode").value(3003))
                .andExpect(jsonPath("$.responseMessage").value("권한이 없습니다."));

        mockMvc.perform(delete("/api/projects/1/members/{memberId}", invitedMemberId.longValue())
                        .header("Authorization", leaderToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.responseCode").value(1000))
                .andExpect(jsonPath("$.result").doesNotExist());

        mockMvc.perform(get("/api/projects/1/members")
                        .header("Authorization", leaderToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.length()").value(1));

    }

    @Test
    void deployedFrontendOriginsAreAllowedForInvitationCorsPreflight() throws Exception {
        assertCorsPreflightAllowed("https://team-pulse-frontend.vercel.app");
        assertCorsPreflightAllowed("https://teampulse.com");
        assertCorsPreflightAllowed("https://www.teampulse.com");
    }

    @Test
    void reportApisUseNotionPathsAndPdfDownload() throws Exception {
        String accessToken = issueAccessToken("report-download@example.com", "Report Owner");

        mockMvc.perform(post("/api/projects")
                        .header("Authorization", accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "projectName": "한글 리포트 프로젝트",
                                  "subject": "고급 프로젝트",
                                  "description": "리포트 API 한글 테스트",
                                  "startDate": "2026-04-01",
                                  "endDate": "2026-06-09"
                                }
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/projects/1/reports")
                        .header("Authorization", accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "reportType": "PDF"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.responseCode").value(3007))
                .andExpect(jsonPath("$.responseMessage").value("\uB9AC\uD3EC\uD2B8\uB97C \uC0DD\uC131\uD560 \uD65C\uB3D9 \uAE30\uB85D\uC774 \uBD80\uC871\uD569\uB2C8\uB2E4."));

        mockMvc.perform(post("/api/projects/1/meetings")
                        .header("Authorization", accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "주간 회의",
                                  "meetingDate": "2026-04-29",
                                  "agenda": "리포트 데이터 수집",
                                  "content": "주간 활동을 정리합니다",
                                  "decisions": "리포트 생성",
                                  "attendeeIds": [],
                                  "actionItems": []
                                }
                                """))
                .andExpect(status().isOk());

        MvcResult reportResult = mockMvc.perform(post("/api/projects/1/reports")
                        .header("Authorization", accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "reportType": "PDF"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.responseCode").value(1000))
                .andExpect(jsonPath("$.result.reportId").isNumber())
                .andExpect(jsonPath("$.result.downloadUrl").isString())
                .andReturn();
        String downloadUrl = JsonPath.read(reportResult.getResponse().getContentAsString(), "$.result.downloadUrl");
        MvcResult downloadResult = mockMvc.perform(get(downloadUrl)
                        .header("Authorization", accessToken))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"teampulse-report.pdf\""))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PDF))
                .andExpect(content().string(startsWith("%PDF-")))
                .andReturn();

        byte[] pdf = downloadResult.getResponse().getContentAsByteArray();
        PdfReader reader = new PdfReader(pdf);
        var extractor = new PdfTextExtractor(reader);
        var extractedText = new StringBuilder();
        for (int page = 1; page <= reader.getNumberOfPages(); page++) {
            extractedText.append(extractor.getTextFromPage(page));
        }
        reader.close();

        assertThat(extractedText.toString())
                .contains("한글 리포트 프로젝트")
                .contains("고급 프로젝트")
                .contains("주간 회의")
                .doesNotContain("???");
    }

    @Test
    void reportPdfIncludesTasksDependenciesMeetingActionsAndRisks() throws Exception {
        String accessToken = issueAccessToken("rich-report@example.com", "Rich Report Owner");

        mockMvc.perform(post("/api/projects")
                        .header("Authorization", accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "projectName": "Rich Report Project",
                                  "subject": "Integration Testing",
                                  "description": "Report branch coverage project",
                                  "startDate": "2026-04-01",
                                  "endDate": "2026-06-09"
                                }
                                """))
                .andExpect(status().isOk());

        MvcResult membersResult = mockMvc.perform(get("/api/projects/1/members")
                        .header("Authorization", accessToken))
                .andExpect(status().isOk())
                .andReturn();
        Number assigneeId = JsonPath.read(membersResult.getResponse().getContentAsString(), "$.result[0].memberId");

        mockMvc.perform(post("/api/projects/1/reports")
                        .header("Authorization", accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "reportType": "HTML"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.isSuccess").value(false));

        MvcResult architectureTaskResult = mockMvc.perform(post("/api/projects/1/tasks")
                        .header("Authorization", accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Architecture Review",
                                  "description": "Capture the design decision",
                                  "assigneeId": %d,
                                  "dueDate": "2026-04-20"
                                }
                                """.formatted(assigneeId.longValue())))
                .andExpect(status().isOk())
                .andReturn();
        Number architectureTaskId = JsonPath.read(architectureTaskResult.getResponse().getContentAsString(), "$.result.taskId");

        MvcResult contractTaskResult = mockMvc.perform(post("/api/projects/1/tasks")
                        .header("Authorization", accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Frontend Contract",
                                  "description": "Align API response fields",
                                  "assigneeId": %d,
                                  "dueDate": "2026-04-21"
                                }
                                """.formatted(assigneeId.longValue())))
                .andExpect(status().isOk())
                .andReturn();
        Number contractTaskId = JsonPath.read(contractTaskResult.getResponse().getContentAsString(), "$.result.taskId");

        mockMvc.perform(post("/api/tasks/{taskId}/dependencies", contractTaskId.longValue())
                        .header("Authorization", accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "precedingTaskId": %d
                                }
                                """.formatted(architectureTaskId.longValue())))
                .andExpect(status().isOk());

        mockMvc.perform(patch("/api/tasks/{taskId}/status", architectureTaskId.longValue())
                        .header("Authorization", accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status": "DOING"
                                }
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/projects/1/meetings")
                        .header("Authorization", accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Weekly Planning",
                                  "meetingDate": "2026-04-29",
                                  "agenda": "Plan report evidence",
                                  "content": "Review test coverage and handoff status",
                                  "decisions": ["Use Postman evidence", "Keep backend API stable"],
                                  "attendeeIds": [%d],
                                  "actionItems": [
                                    {
                                      "content": "Collect screenshots",
                                      "assigneeId": %d,
                                      "dueDate": "2026-04-30"
                                    }
                                  ]
                                }
                                """.formatted(assigneeId.longValue(), assigneeId.longValue())))
                .andExpect(status().isOk());

        MvcResult reportResult = mockMvc.perform(post("/api/projects/1/reports")
                        .header("Authorization", accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "reportType": "PDF"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result.downloadUrl").isString())
                .andReturn();
        String downloadUrl = JsonPath.read(reportResult.getResponse().getContentAsString(), "$.result.downloadUrl");

        MvcResult downloadResult = mockMvc.perform(get(downloadUrl)
                        .header("Authorization", accessToken))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PDF))
                .andReturn();

        byte[] pdf = downloadResult.getResponse().getContentAsByteArray();
        PdfReader reader = new PdfReader(pdf);
        var extractor = new PdfTextExtractor(reader);
        var extractedText = new StringBuilder();
        for (int page = 1; page <= reader.getNumberOfPages(); page++) {
            extractedText.append(extractor.getTextFromPage(page));
        }
        reader.close();

        assertThat(extractedText.toString())
                .contains("Rich Report Project")
                .contains("Architecture Review")
                .contains("Frontend Contract")
                .contains("Weekly Planning")
                .contains("Decisions:")
                .contains("Action items:")
                .contains("Risk Signals");
    }

    private String issueAccessToken(String email) throws Exception {
        return issueAccessToken(email, "Project List Owner");
    }

    private String issueAccessToken(String email, String name) throws Exception {
        MvcResult signupResult = mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "%s",
                                  "password": "!aaa123123",
                                  "name": "%s",
                                  "university": "Konkuk University",
                                  "phone": "010-1234-5678"
                                }
                                """.formatted(email, name)))
                .andExpect(status().isOk())
                .andReturn();
        return JsonPath.read(signupResult.getResponse().getContentAsString(), "$.result.jwtInfo.accessToken");
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
