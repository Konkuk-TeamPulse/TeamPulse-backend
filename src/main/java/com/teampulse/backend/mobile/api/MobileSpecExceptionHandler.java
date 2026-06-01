package com.teampulse.backend.mobile.api;

import com.teampulse.backend.common.api.SpecResponse;
import com.teampulse.backend.project.api.ProjectMemberApiController;
import java.util.List;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(assignableTypes = {
        ProjectApiController.class,
        ProjectMeetingApiController.class,
        MeetingApiController.class,
        InvitationApiController.class,
        ProjectMemberApiController.class,
        ProjectTaskApiController.class,
        TaskApiController.class
})
public class MobileSpecExceptionHandler {

    private static final String VALIDATION_MESSAGE = "요청 값이 올바르지 않습니다.";

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<SpecResponse<Void>> handleIllegalArgument(IllegalArgumentException exception) {
        return ResponseEntity.badRequest()
                .body(SpecResponse.fail(responseCode(exception.getMessage()), responseMessage(exception.getMessage()), null));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<SpecResponse<Void>> handleAccessDenied(AccessDeniedException exception) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(SpecResponse.fail(3003, "권한이 없습니다.", null));
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
        if ("Project not found.".equals(message)) {
            return 4001;
        }
        if ("Member not found.".equals(message) || "Assignee not found.".equals(message)) {
            return 4002;
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
        if ("Risk not found.".equals(message)) {
            return 4007;
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
        if ("Project not found.".equals(message)) {
            return "프로젝트를 찾을 수 없습니다.";
        }
        if ("Member not found.".equals(message) || "Assignee not found.".equals(message)) {
            return "팀원을 찾을 수 없습니다.";
        }
        if ("Task not found.".equals(message)) {
            return "태스크를 찾을 수 없습니다.";
        }
        if ("Meeting not found.".equals(message)) {
            return "회의록을 찾을 수 없습니다.";
        }
        if ("Report data is insufficient.".equals(message)) {
            return "리포트를 생성할 활동 기록이 부족합니다.";
        }
        if ("Report not found.".equals(message)) {
            return "리포트를 찾을 수 없습니다.";
        }
        if ("Risk not found.".equals(message)) {
            return "리스크를 찾을 수 없습니다.";
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
