package com.teampulse.backend.common.exception;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.lang.reflect.Method;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void illegalArgumentReturnsInvalidInputApiResponse() {
        var response = handler.handleIllegalArgument(new IllegalArgumentException("Bad input."));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().success()).isFalse();
        assertThat(response.getBody().error().code()).isEqualTo("INVALID_INPUT");
        assertThat(response.getBody().error().message()).isEqualTo("Bad input.");
        assertThat(response.getBody().error().details()).isNull();
    }

    @Test
    void validationReturnsFieldErrorDetails() throws Exception {
        var exception = validationException(
                new FieldError("request", "name", "", false, null, null, "Name is required."),
                new FieldError("request", "email", "bad", false, null, null, "Email must be valid.")
        );

        var response = handler.handleValidation(exception);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().success()).isFalse();
        assertThat(response.getBody().error().code()).isEqualTo("VALIDATION_FAILED");
        assertThat(response.getBody().error().message()).isEqualTo("Request validation failed.");
        assertThat(response.getBody().error().details())
                .asList()
                .containsExactly("name: Name is required.", "email: Email must be valid.");
    }

    @Test
    void unexpectedExceptionReturnsInternalServerErrorApiResponse() {
        var response = handler.handleException(new RuntimeException("boom"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().success()).isFalse();
        assertThat(response.getBody().error().code()).isEqualTo("INTERNAL_SERVER_ERROR");
        assertThat(response.getBody().error().message()).isEqualTo("Unexpected server error.");
    }

    private MethodArgumentNotValidException validationException(FieldError... errors) throws Exception {
        var target = new ValidationTarget("");
        var bindingResult = new BeanPropertyBindingResult(target, "request");
        for (var error : errors) {
            bindingResult.addError(error);
        }
        Method method = GlobalExceptionHandlerTest.class.getDeclaredMethod("validate", ValidationTarget.class);
        return new MethodArgumentNotValidException(new MethodParameter(method, 0), bindingResult);
    }

    @SuppressWarnings("unused")
    private void validate(@Valid ValidationTarget target) {
    }

    private record ValidationTarget(@NotBlank String name) {
    }
}
