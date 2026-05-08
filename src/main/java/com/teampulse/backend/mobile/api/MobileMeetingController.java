package com.teampulse.backend.mobile.api;


import com.teampulse.backend.mobile.application.*;
import com.teampulse.backend.mobile.dto.*;
import com.teampulse.backend.common.api.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/mobile/meetings")
public class MobileMeetingController {

    private final MobileMeetingUseCase mobileMeetingUseCase;

    public MobileMeetingController(MobileMeetingUseCase mobileMeetingUseCase) {
        this.mobileMeetingUseCase = mobileMeetingUseCase;
    }

    // Deprecated: 프론트엔드는 프로젝트 기반 회의 생성 API POST /api/projects/{projectId}/meetings 를 사용합니다.
    @Deprecated
    @PostMapping
    public ApiResponse<WorkspaceState> createMeeting(@Valid @RequestBody CreateMeetingRequest request) {
        return ApiResponse.ok(mobileMeetingUseCase.createMeeting(request));
    }
}
