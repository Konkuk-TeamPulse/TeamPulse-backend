package com.teampulse.backend.mobile.application;

import com.teampulse.backend.mobile.dto.UpdateAccountRequest;
import com.teampulse.backend.mobile.dto.WorkspaceState;

public interface MobileAccountUseCase {

    WorkspaceState updateAccount(UpdateAccountRequest request);
}
