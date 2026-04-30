package com.teampulse.backend.auth.api;

import com.teampulse.backend.auth.application.AuthUserNotFoundException;
import com.teampulse.backend.auth.application.DuplicateEmailException;
import com.teampulse.backend.auth.application.InvalidPasswordException;
import com.teampulse.backend.auth.application.InvalidRefreshTokenException;
import com.teampulse.backend.common.api.SpecResponse;
import java.util.List;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(assignableTypes = AuthApiController.class)
public class AuthExceptionHandler {

    private static final String DUPLICATE_EMAIL_MESSAGE = "\uC911\uBCF5\uB41C \uC774\uBA54\uC77C\uC785\uB2C8\uB2E4.";
    private static final String USER_NOT_FOUND_MESSAGE = "\uC874\uC7AC\uD558\uC9C0 \uC54A\uB294 \uD68C\uC6D0\uC785\uB2C8\uB2E4.";
    private static final String INVALID_PASSWORD_MESSAGE = "\uBE44\uBC00\uBC88\uD638\uAC00 \uC77C\uCE58\uD558\uC9C0 \uC54A\uC2B5\uB2C8\uB2E4.";
    private static final String VALIDATION_MESSAGE = "\uC694\uCCAD \uAC12\uC774 \uC798\uBABB\uB418\uC5C8\uC2B5\uB2C8\uB2E4.";

    @ExceptionHandler(DuplicateEmailException.class)
    public ResponseEntity<SpecResponse<Void>> handleDuplicateEmail(DuplicateEmailException exception) {
        return ResponseEntity.status(409)
                .body(SpecResponse.fail(2012, DUPLICATE_EMAIL_MESSAGE, null));
    }

    @ExceptionHandler(AuthUserNotFoundException.class)
    public ResponseEntity<SpecResponse<Void>> handleUserNotFound(AuthUserNotFoundException exception) {
        return ResponseEntity.status(404)
                .body(SpecResponse.fail(2013, USER_NOT_FOUND_MESSAGE, null));
    }

    @ExceptionHandler(InvalidPasswordException.class)
    public ResponseEntity<SpecResponse<Void>> handleInvalidPassword(InvalidPasswordException exception) {
        return ResponseEntity.status(401)
                .body(SpecResponse.fail(2014, INVALID_PASSWORD_MESSAGE, null));
    }

    @ExceptionHandler(InvalidRefreshTokenException.class)
    public ResponseEntity<SpecResponse<Void>> handleInvalidRefreshToken(InvalidRefreshTokenException exception) {
        return ResponseEntity.status(401)
                .body(SpecResponse.fail(2009, exception.getMessage(), null));
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
                .body(SpecResponse.fail(2020, VALIDATION_MESSAGE, new ValidationErrorResponse(errors)));
    }

    public record ValidationErrorResponse(List<FieldErrorResponse> errors) {
    }

    public record FieldErrorResponse(String fieldName, Object rejectValue, String message) {
    }
}
