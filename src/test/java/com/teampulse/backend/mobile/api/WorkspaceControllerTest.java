package com.teampulse.backend.mobile.api;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;

import com.jayway.jsonpath.JsonPath;
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

    @Test
    void accountActivitiesAndRiskActionsExposeApiSpecRefinements() throws Exception {
        mockMvc.perform(post("/api/mobile/workspace/sample"))
                .andExpect(status().isOk());

        mockMvc.perform(patch("/api/account")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Lee Juho",
                                  "email": "juho@example.com",
                                  "university": "Konkuk University",
                                  "phone": "010-1111-2222"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.email").value("juho@example.com"))
                .andExpect(jsonPath("$.data.university").value("Konkuk University"))
                .andExpect(jsonPath("$.data.phone").value("010-1111-2222"));

        mockMvc.perform(get("/api/projects/1/activities"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].updatedAt").exists());

        mockMvc.perform(get("/api/account/activities"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].actor").value("Lee Juho"))
                .andExpect(jsonPath("$.data[0].updatedAt").exists());

        mockMvc.perform(get("/api/projects/1/risks/101/actions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].type").value("RESCHEDULE"));
    }

    @Test
    void projectApisUseNotionSpecRequestAndResponseShape() throws Exception {
        mockMvc.perform(post("/api/mobile/workspace/reset"))
                .andExpect(status().isOk());

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

        mockMvc.perform(get("/api/projects/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.result.projectId").value(1))
                .andExpect(jsonPath("$.result.projectName").value("TeamPulse Project"))
                .andExpect(jsonPath("$.result.subject").value("Advanced Project"))
                .andExpect(jsonPath("$.result.description").value("TeamPulse backend implementation"))
                .andExpect(jsonPath("$.result.startDate").value("2026-04-01"))
                .andExpect(jsonPath("$.result.endDate").value("2026-06-09"))
                .andExpect(jsonPath("$.result.memberCount").value(1));

        mockMvc.perform(patch("/api/projects/1")
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
        mockMvc.perform(post("/api/mobile/workspace/reset"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/projects")
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

        MvcResult signupResult = mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "me-lookup@example.com",
                                  "password": "!aaa123123",
                                  "name": "Account Owner",
                                  "university": "Konkuk University",
                                  "phone": "010-9999-0000"
                                }
                                """))
                .andExpect(status().isOk())
                .andReturn();
        String accessToken = JsonPath.read(signupResult.getResponse().getContentAsString(), "$.result.jwtInfo.accessToken");

        mockMvc.perform(get("/api/users/me")
                        .header("Authorization", accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.responseCode").value(1000))
                .andExpect(jsonPath("$.result.userId").isNumber())
                .andExpect(jsonPath("$.result.email").value("me-lookup@example.com"))
                .andExpect(jsonPath("$.result.name").value("Account Owner"))
                .andExpect(jsonPath("$.result.university").value("Konkuk University"))
                .andExpect(jsonPath("$.result.phone").value("010-9999-0000"));

        mockMvc.perform(get("/api/projects/1/dashboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.responseCode").value(1000))
                .andExpect(jsonPath("$.result.projectId").value(1))
                .andExpect(jsonPath("$.result.projectName").value("Progressing API Project"))
                .andExpect(jsonPath("$.result.taskSummary.totalTaskCount").value(0))
                .andExpect(jsonPath("$.result.scheduleSummary.projectStartDate").value("2026-04-01"))
                .andExpect(jsonPath("$.result.memberWorkload[0].name").value("Demo Leader"))
                .andExpect(jsonPath("$.result.riskSummary.totalRiskCount").isNumber());

        mockMvc.perform(get("/api/projects/1/activity-logs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.responseCode").value(1000))
                .andExpect(jsonPath("$.result[0].logId").isNumber())
                .andExpect(jsonPath("$.result[0].content").exists())
                .andExpect(jsonPath("$.result[0].userName").value("Demo Leader"));

        mockMvc.perform(get("/api/projects/1/members"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.responseCode").value(1000))
                .andExpect(jsonPath("$.result[0].memberId").isNumber())
                .andExpect(jsonPath("$.result[0].email").value("leader@teampulse.app"))
                .andExpect(jsonPath("$.result[0].role").value("LEADER"));

        mockMvc.perform(delete("/api/projects/1/members/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.responseCode").value(1000))
                .andExpect(jsonPath("$.result").doesNotExist());
    }

    @Test
    void taskApisUseNotionSpecRequestAndResponseShape() throws Exception {
        mockMvc.perform(post("/api/mobile/workspace/reset"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/projects")
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

        MvcResult membersResult = mockMvc.perform(get("/api/projects/1/members"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andReturn();
        Number assigneeId = JsonPath.read(membersResult.getResponse().getContentAsString(), "$.result[0].memberId");

        MvcResult createResult = mockMvc.perform(post("/api/projects/1/tasks")
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
                        .with(user("tester"))
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
                .andExpect(jsonPath("$.result.precedingTaskId").value(precedingTaskId.longValue()));

        mockMvc.perform(get("/api/projects/1/tasks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.result[?(@.taskId == %d)].precedingTaskIds[0]".formatted(taskId.longValue()))
                        .value(hasItem(precedingTaskId.intValue())));

        mockMvc.perform(post("/api/tasks/{taskId}/dependencies", taskId.longValue())
                        .with(user("tester"))
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
                        .with(user("tester")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.responseCode").value(1000))
                .andExpect(jsonPath("$.result").doesNotExist());

        mockMvc.perform(get("/api/projects/1/tasks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.responseCode").value(1000))
                .andExpect(jsonPath("$.result[?(@.taskId == %d)].title".formatted(taskId.longValue())).value(hasItem("Write API spec")))
                .andExpect(jsonPath("$.result[?(@.taskId == %d)].assigneeName".formatted(taskId.longValue())).value(hasItem("Demo Leader")))
                .andExpect(jsonPath("$.result[?(@.taskId == %d)].dueDate".formatted(taskId.longValue())).value(hasItem("2026-04-28")));

        mockMvc.perform(patch("/api/tasks/{taskId}", taskId.longValue())
                        .with(user("tester"))
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
                        .with(user("tester"))
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

        mockMvc.perform(delete("/api/tasks/{taskId}", taskId.longValue())
                        .with(user("tester")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.responseCode").value(1000))
                .andExpect(jsonPath("$.result").doesNotExist());
    }

    @Test
    void meetingApisUseSpecResponseShape() throws Exception {
        mockMvc.perform(post("/api/mobile/workspace/reset"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/projects")
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

        MvcResult membersResult = mockMvc.perform(get("/api/projects/1/members"))
                .andExpect(status().isOk())
                .andReturn();
        Number memberId = JsonPath.read(membersResult.getResponse().getContentAsString(), "$.result[0].memberId");
        String accessToken = issueAccessToken("meeting-detail@example.com");

        MvcResult createResult = mockMvc.perform(post("/api/projects/1/meetings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Weekly Sync",
                                  "meetingDate": "2026-04-29",
                                  "agenda": "Review backend progress",
                                  "content": "Discuss remaining in-progress APIs",
                                  "decisions": "Keep Notion API shape",
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

        mockMvc.perform(get("/api/projects/1/meetings"))
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

        mockMvc.perform(get("/api/projects/1/meetings/{meetingId}", meetingId.longValue()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.responseCode").value(1000))
                .andExpect(jsonPath("$.result.meetingId").value(meetingId.longValue()))
                .andExpect(jsonPath("$.result.actions[0]").value("Implement meeting APIs"))
                .andExpect(jsonPath("$.result.content").value("Discuss remaining in-progress APIs"))
                .andExpect(jsonPath("$.result.attendeeIds[0]").value(memberId.longValue()))
                .andExpect(jsonPath("$.result.actionItems[0].assigneeId").value(memberId.longValue()));

        mockMvc.perform(get("/api/meetings/{meetingId}", meetingId.longValue())
                        .header("Authorization", accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.responseCode").value(1000))
                .andExpect(jsonPath("$.result.meetingId").value(meetingId.longValue()))
                .andExpect(jsonPath("$.result.projectId").value(1))
                .andExpect(jsonPath("$.result.meetingDate").value("2026-04-29"))
                .andExpect(jsonPath("$.result.attendees[0].memberId").value(memberId.longValue()))
                .andExpect(jsonPath("$.result.attendees[0].name").value("Demo Leader"))
                .andExpect(jsonPath("$.result.actionItems[0].assigneeMemberId").value(memberId.longValue()))
                .andExpect(jsonPath("$.result.actionItems[0].assigneeName").value("Demo Leader"))
                .andExpect(jsonPath("$.result.createdAt").exists())
                .andExpect(jsonPath("$.result.updatedAt").exists());
    }

    @Test
    void invitationApisUseNotionPathsAndSpecResponseShape() throws Exception {
        mockMvc.perform(post("/api/mobile/workspace/reset"))
                .andExpect(status().isOk());

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

        mockMvc.perform(post("/api/invitations/{inviteCode}/accept", inviteCode)
                        .header("Authorization", accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.responseCode").value(1000))
                .andExpect(jsonPath("$.result.memberId").isNumber())
                .andExpect(jsonPath("$.result.projectId").value(1))
                .andExpect(jsonPath("$.result.projectName").value("Invitation Project"))
                .andExpect(jsonPath("$.result.role").value("MEMBER"));
    }

    @Test
    void reportApisUseNotionPathsAndPdfDownload() throws Exception {
        mockMvc.perform(post("/api/mobile/workspace/reset"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "projectName": "Report Project",
                                  "subject": "Advanced Project",
                                  "description": "Report API test project",
                                  "startDate": "2026-04-01",
                                  "endDate": "2026-06-09"
                                }
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/projects/1/reports")
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
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Report Source Meeting",
                                  "meetingDate": "2026-04-29",
                                  "agenda": "Collect report data",
                                  "content": "Gather weekly activity",
                                  "decisions": "Create report",
                                  "attendeeIds": [],
                                  "actionItems": []
                                }
                                """))
                .andExpect(status().isOk());

        MvcResult reportResult = mockMvc.perform(post("/api/projects/1/reports")
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
        String accessToken = issueAccessToken("report-download@example.com");

        mockMvc.perform(get(downloadUrl)
                        .header("Authorization", accessToken))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"teampulse-report.pdf\""))
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PDF))
                .andExpect(content().string(startsWith("%PDF-1.4")));
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
}
