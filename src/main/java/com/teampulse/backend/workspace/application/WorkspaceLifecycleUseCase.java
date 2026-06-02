package com.teampulse.backend.workspace.application;


import com.teampulse.backend.workspace.dto.*;
public interface WorkspaceLifecycleUseCase {

    WorkspaceState reset();

    WorkspaceState bootstrap(BootstrapWorkspaceRequest request);

    WorkspaceState loadSample();
}
