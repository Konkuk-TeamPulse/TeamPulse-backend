package com.teampulse.backend.mobile.api;

import com.teampulse.backend.common.api.SpecResponse;
import java.util.List;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(assignableTypes = {
        ProjectApiController.class,
        ProjectMeetingApiController.class,
        MeetingApiController.class,
        InvitationApiController.class,
        ProjectTaskApiController.class,
        TaskApiController.class
})
public class MobileSpecExceptionHandler {

    private static final String VALIDATION_MESSAGE = "요청 값이 올바르지 않습니다.";

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<SpecResponse<Void>> handleIllegalArgument(IllegalArgumentException exception) {
        var message = exception.getMessage();
        var status = switch (message == null ? "" : message) {
            case "Authentication user is required." -> 401;
            case "Project leader permission is required.", "Current user is not a project member." -> 403;
            default -> 400;
        };
        return ResponseEntity.status(status)
                .body(SpecResponse.fail(responseCode(message), responseMessage(message), null));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<SpecResponse<ValidationErrorResponse>> handleValidation(MethodArgumentNotValidException exception) {
        var errors = exception.getBindingResult().getFieldErrors().stream()
                .map(error -> new FieldErrorResponse(
                        error.getField(),
                        error.getRejectedValue(),
                        error.getDefaultMessage()))
                .toList();
        return ResponseEntity.badRequest()
                .body(SpecResponse.fail(2020, validationMessage(errors), new ValidationErrorResponse(errors)));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<SpecResponse<Void>> handleUnreadableMessage(HttpMessageNotReadableException exception) {
        return ResponseEntity.badRequest()
                .body(SpecResponse.fail(2020, "요청 본문이 올바르지 않습니다.", null));
    }

    private int responseCode(String message) {
        if ("Task cannot depend on itself.".equals(message)
                || "Task dependency cycle is not allowed.".equals(message)) {
            return 3006;
        }
        if ("Task not found.".equals(message)) {
            return 4004;
        }
        if ("Meeting not found.".equals(message)) {
            return 4005;
        }
        if ("Report data is insufficient.".equals(message)) {
            return 3007;
        }
        if ("Report not found.".equals(message)) {
            return 4006;
        }
        if ("Project leader permission is required.".equals(message)) {
            return 3008;
        }
        if ("Current user is not a project member.".equals(message)) {
            return 3008;
        }
        if ("Authentication user is required.".equals(message)) {
            return 3001;
        }
        if (message != null && message.contains("project 1")) {
            return 3002;
        }
        return 2020;
    }

    private String responseMessage(String message) {
        if ("Task cannot depend on itself.".equals(message)) {
            return "자기 자신을 선행 태스크로 설정할 수 없습니다.";
        }
        if ("Task dependency cycle is not allowed.".equals(message)) {
            return "순환되는 태스크 의존관계는 설정할 수 없습니다.";
        }
        if ("Report data is insufficient.".equals(message)) {
            return "리포트를 생성할 활동 기록이 부족합니다.";
        }
        if ("Project leader permission is required.".equals(message)) {
            return "프로젝트 팀장 권한이 필요합니다.";
        }
        if ("Current user is not a project member.".equals(message)) {
            return "프로젝트 접근 권한이 없습니다.";
        }
        if ("Authentication user is required.".equals(message)) {
            return "로그인이 필요합니다.";
        }
        return message == null || message.isBlank() ? VALIDATION_MESSAGE : message;
    }

    private String validationMessage(List<FieldErrorResponse> errors) {
        if (errors.stream().anyMatch(error -> "title".equals(error.fieldName()))
                && errors.stream().anyMatch(error -> "meetingDate".equals(error.fieldName()))) {
            return "회의 제목과 회의 일자는 필수입니다.";
        }
        return VALIDATION_MESSAGE;
    }

    public record ValidationErrorResponse(List<FieldErrorResponse> errors) {
    }

    public record FieldErrorResponse(String fieldName, Object rejectValue, String message) {
    }
}
