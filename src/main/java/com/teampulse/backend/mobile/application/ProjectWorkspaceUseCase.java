package com.teampulse.backend.mobile.application;

import com.teampulse.backend.mobile.dto.BootstrapWorkspaceRequest;
import com.teampulse.backend.mobile.dto.CreateMeetingRequest;
import com.teampulse.backend.mobile.dto.CreateMemberRequest;
import com.teampulse.backend.mobile.dto.CreateTaskRequest;
import com.teampulse.backend.mobile.dto.TaskDependencyRequest;
import com.teampulse.backend.mobile.dto.UpdateTaskRequest;
import com.teampulse.backend.mobile.dto.UpdateTaskStatusRequest;
import com.teampulse.backend.mobile.dto.UpdateTeamRequest;
import com.teampulse.backend.mobile.dto.WorkspaceState;
import java.util.List;

public interface ProjectWorkspaceUseCase {

    List<WorkspaceState> getProjectWorkspaces();

    WorkspaceState getProjectWorkspace(long projectId);

    WorkspaceState getProjectWorkspaceByTaskId(long taskId);

    WorkspaceState getProjectWorkspaceByMeetingId(long meetingId);

    WorkspaceState getProjectWorkspaceByReportId(long reportId);

    WorkspaceState createProjectWorkspace(BootstrapWorkspaceRequest request);

    WorkspaceState resetProjectWorkspace(long projectId);

    WorkspaceState updateProjectTeam(long projectId, UpdateTeamRequest request);

    WorkspaceState regenerateProjectInviteCode(long projectId);

    WorkspaceState addProjectMember(long projectId, CreateMemberRequest request);

    WorkspaceState deleteProjectMember(long projectId, long memberId);

    WorkspaceState createProjectTask(long projectId, CreateTaskRequest request);

    WorkspaceState updateProjectTask(long projectId, long taskId, UpdateTaskRequest request);

    WorkspaceState updateProjectTaskStatus(long projectId, long taskId, UpdateTaskStatusRequest request);

    WorkspaceState deleteProjectTask(long projectId, long taskId);

    WorkspaceState addProjectTaskDependency(long projectId, long taskId, TaskDependencyRequest request);

    WorkspaceState deleteProjectTaskDependency(long projectId, long taskId, String dependencyTitle);

    WorkspaceState updateTaskById(long taskId, UpdateTaskRequest request);

    WorkspaceState updateTaskStatusById(long taskId, UpdateTaskStatusRequest request);

    WorkspaceState deleteTaskById(long taskId);

    WorkspaceState addTaskDependencyById(long taskId, TaskDependencyRequest request);

    WorkspaceState deleteTaskDependencyById(long taskId, String dependencyTitle);

    WorkspaceState createProjectMeeting(long projectId, CreateMeetingRequest request);

    WorkspaceState generateProjectReport(long projectId);
}
