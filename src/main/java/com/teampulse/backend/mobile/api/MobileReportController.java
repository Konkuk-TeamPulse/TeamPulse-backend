package com.teampulse.backend.mobile.api;


import com.teampulse.backend.mobile.application.*;
import com.teampulse.backend.mobile.dto.*;
import com.teampulse.backend.common.api.ApiResponse;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/mobile/reports")
public class MobileReportController {

    private final MobileReportUseCase mobileReportUseCase;

    public MobileReportController(MobileReportUseCase mobileReportUseCase) {
        this.mobileReportUseCase = mobileReportUseCase;
    }

    // Deprecated: 프론트엔드는 프로젝트 리포트 생성 API POST /api/projects/{projectId}/reports 를 사용합니다.
    @Deprecated
    @PostMapping
    public ApiResponse<WorkspaceState> generateReport() {
        return ApiResponse.ok(mobileReportUseCase.generateReport());
    }
}
