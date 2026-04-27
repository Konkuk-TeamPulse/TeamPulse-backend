package com.teampulse.backend.mobile.application;


import com.teampulse.backend.mobile.dto.*;
public interface WorkspaceLifecycleUseCase {

    WorkspaceState reset();

    WorkspaceState bootstrap(BootstrapWorkspaceRequest request);

    WorkspaceState loadSample();
}
