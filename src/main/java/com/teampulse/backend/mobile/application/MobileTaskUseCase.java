package com.teampulse.backend.mobile.application;


import com.teampulse.backend.mobile.dto.*;
public interface MobileTaskUseCase {

    WorkspaceState createTask(CreateTaskRequest request);

    WorkspaceState updateTaskStatus(long taskId, UpdateTaskStatusRequest request);

    WorkspaceState deleteTask(long taskId);
}
