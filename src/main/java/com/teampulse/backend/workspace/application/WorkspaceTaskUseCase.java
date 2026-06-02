package com.teampulse.backend.workspace.application;


import com.teampulse.backend.workspace.dto.*;
public interface WorkspaceTaskUseCase {

    WorkspaceState createTask(CreateTaskRequest request);

    WorkspaceState updateTask(long taskId, UpdateTaskRequest request);

    WorkspaceState updateTaskStatus(long taskId, UpdateTaskStatusRequest request);

    WorkspaceState deleteTask(long taskId);

    WorkspaceState addTaskDependency(long taskId, TaskDependencyRequest request);

    WorkspaceState deleteTaskDependency(long taskId, String dependencyTitle);
}
