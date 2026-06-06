package com.teampulse.backend.workspace.api;

import com.teampulse.backend.common.api.SpecResponse;
import com.teampulse.backend.workspace.application.ProjectWorkspaceUseCase;
import com.teampulse.backend.workspace.dto.CreateTaskRequest;
import com.teampulse.backend.workspace.dto.MemberView;
import com.teampulse.backend.workspace.dto.TaskCreateSpecRequest;
import com.teampulse.backend.workspace.dto.TaskCreateSpecResponse;
import com.teampulse.backend.workspace.dto.TaskSummarySpecResponse;
import com.teampulse.backend.workspace.dto.TaskView;
import com.teampulse.backend.workspace.dto.WorkspaceState;
import jakarta.validation.Valid;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/projects/{projectId}/tasks")
public class ProjectTaskApiController {

    private static final String SUCCESS_MESSAGE = "\uC694\uCCAD\uC5D0 \uC131\uACF5\uD588\uC2B5\uB2C8\uB2E4.";
    private static final String TASK_CREATED_MESSAGE = "\uD0DC\uC2A4\uD06C\uAC00 \uC0DD\uC131\uB418\uC5C8\uC2B5\uB2C8\uB2E4.";

    private final ProjectWorkspaceUseCase projectWorkspaceUseCase;

    public ProjectTaskApiController(ProjectWorkspaceUseCase projectWorkspaceUseCase) {
        this.projectWorkspaceUseCase = projectWorkspaceUseCase;
    }

    @GetMapping
    public SpecResponse<List<TaskSummarySpecResponse>> listTasks(@PathVariable long projectId) {
        var workspace = projectWorkspaceUseCase.getProjectWorkspace(projectId);
        var taskIdsByTitle = workspace.tasks().stream()
                .collect(Collectors.groupingBy(TaskView::title, Collectors.mapping(TaskView::id, Collectors.toList())));
        var tasks = workspace.tasks().stream()
                .map(task -> {
                    var assignee = findAssignee(workspace, task);
                    return new TaskSummarySpecResponse(
                            task.id(),
                            task.title(),
                            task.status(),
                            assignee == null ? task.assigneeId() : assignee.id(),
                            task.owner(),
                            assignee == null ? null : assignee.email(),
                            task.dueDate(),
                            precedingTaskIds(task, taskIdsByTitle),
                            blockedTaskIds(task, workspace.tasks()));
                })
                .toList();
        return SpecResponse.ok(SUCCESS_MESSAGE, tasks);
    }

    @PostMapping
    public SpecResponse<TaskCreateSpecResponse> createTask(
            @PathVariable long projectId,
            @Valid @RequestBody TaskCreateSpecRequest request
    ) {
        var workspace = projectWorkspaceUseCase.getProjectWorkspace(projectId);
        var assignee = requireMemberById(workspace, request.assigneeId());
        requireMatchingEmail(assignee, request.assigneeEmail());
        var updated = projectWorkspaceUseCase.createProjectTask(projectId, new CreateTaskRequest(
                request.title(),
                assignee.name(),
                request.dueDate(),
                List.of(),
                normalizeNullable(request.description()),
                assignee.id()));
        var task = latestTask(updated);
        return SpecResponse.ok(TASK_CREATED_MESSAGE, new TaskCreateSpecResponse(
                task.id(),
                task.title(),
                task.status(),
                assignee.id(),
                task.dueDate()));
    }

    private MemberView requireMemberById(WorkspaceState workspace, long memberId) {
        return workspace.members().stream()
                .filter(member -> member.id() == memberId)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Assignee not found."));
    }

    private void requireMatchingEmail(MemberView assignee, String email) {
        if (!assignee.email().equalsIgnoreCase(email.trim())) {
            throw new IllegalArgumentException("Assignee id and email do not match.");
        }
    }

    private MemberView findAssignee(WorkspaceState workspace, TaskView task) {
        if (task.assigneeId() != null) {
            return workspace.members().stream()
                    .filter(member -> member.id() == task.assigneeId())
                    .findFirst()
                    .orElse(null);
        }
        return workspace.members().stream()
                .filter(member -> member.name().equalsIgnoreCase(task.owner()))
                .findFirst()
                .orElse(null);
    }

    private TaskView latestTask(WorkspaceState workspace) {
        return workspace.tasks().stream()
                .max(Comparator.comparingLong(TaskView::id))
                .orElseThrow(() -> new IllegalArgumentException("Task not found."));
    }

    private String normalizeNullable(String value) {
        return value == null ? "" : value.trim();
    }

    private List<Long> precedingTaskIds(TaskView task, Map<String, List<Long>> taskIdsByTitle) {
        return task.blockers().stream()
                .flatMap(blocker -> taskIdsByTitle.getOrDefault(blocker, List.of()).stream())
                .filter(taskId -> taskId != task.id())
                .distinct()
                .toList();
    }

    private List<Long> blockedTaskIds(TaskView task, List<TaskView> tasks) {
        return tasks.stream()
                .filter(candidate -> candidate.id() != task.id())
                .filter(candidate -> candidate.blockers().stream()
                        .anyMatch(blocker -> blocker.equalsIgnoreCase(task.title())))
                .map(TaskView::id)
                .distinct()
                .toList();
    }

}
