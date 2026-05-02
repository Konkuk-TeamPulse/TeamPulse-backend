package com.teampulse.backend.mobile.api;

import com.teampulse.backend.common.api.SpecResponse;
import java.util.List;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
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

    private static final String VALIDATION_MESSAGE = "요청 값이 잘못되었습니다.";

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<SpecResponse<Void>> handleIllegalArgument(IllegalArgumentException exception) {
        return ResponseEntity.badRequest()
                .body(SpecResponse.fail(responseCode(exception.getMessage()), responseMessage(exception.getMessage()), null));
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

    private int responseCode(String message) {
        if ("Task cannot depend on itself.".equals(message)) {
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
        if (message != null && message.contains("project 1")) {
            return 3002;
        }
        return 2020;
    }

    private String responseMessage(String message) {
        if ("Task cannot depend on itself.".equals(message)) {
            return "자기 자신을 선행 태스크로 설정할 수 없습니다.";
        }
        if ("Report data is insufficient.".equals(message)) {
            return "\uB9AC\uD3EC\uD2B8\uB97C \uC0DD\uC131\uD560 \uD65C\uB3D9 \uAE30\uB85D\uC774 \uBD80\uC871\uD569\uB2C8\uB2E4.";
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
