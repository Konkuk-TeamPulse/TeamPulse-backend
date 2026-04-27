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

    @PostMapping
    public ApiResponse<WorkspaceState> generateReport() {
        return ApiResponse.ok(mobileReportUseCase.generateReport());
    }
}
