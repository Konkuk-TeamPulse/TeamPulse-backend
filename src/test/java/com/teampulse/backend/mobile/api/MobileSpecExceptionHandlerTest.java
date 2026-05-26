package com.teampulse.backend.mobile.api;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import java.lang.reflect.Method;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

class MobileSpecExceptionHandlerTest {

    private final MobileSpecExceptionHandler handler = new MobileSpecExceptionHandler();

    @Test
    void mapsKnownIllegalArgumentMessagesToSpecCodes() {
        assertFailure("Task cannot depend on itself.", 3006);
        assertFailure("Task dependency cycle is not allowed.", 3006);
        assertFailure("Project not found.", 4001);
        assertFailure("Member not found.", 4002);
        assertFailure("Assignee not found.", 4002);
        assertFailure("Task not found.", 4004);
        assertFailure("Meeting not found.", 4005);
        assertFailure("Report data is insufficient.", 3007);
        assertFailure("Report not found.", 4006);
        assertFailure("Risk not found.", 4007);
        assertFailure("Task does not belong to project 1.", 3002);
    }

    @Test
    void mapsUnknownBlankAndNullIllegalArgumentMessagesToValidationFallback() {
        assertFailure("Anything else.", 2020, "Anything else.");
        assertFailure("", 2020);
        assertFailure(null, 2020);
    }

    @Test
    void validationResponseContainsFieldErrorsAndMeetingSpecificMessage() throws Exception {
        var exception = validationException(
                new FieldError("request", "title", "", false, null, null, "Title is required."),
                new FieldError("request", "meetingDate", "", false, null, null, "Meeting date is required.")
        );

        var response = handler.handleValidation(exception);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isFalse();
        assertThat(response.getBody().responseCode()).isEqualTo(2020);
        assertThat(response.getBody().result().errors()).hasSize(2);
        assertThat(response.getBody().result().errors())
                .extracting(MobileSpecExceptionHandler.FieldErrorResponse::fieldName)
                .containsExactly("title", "meetingDate");
        assertThat(response.getBody().responseMessage()).isNotBlank();
    }

    @Test
    void validationResponseUsesGenericMessageForOtherFields() throws Exception {
        var exception = validationException(new FieldError(
                "request",
                "projectName",
                "",
                false,
                null,
                null,
                "Project name is required."
        ));

        var response = handler.handleValidation(exception);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().responseCode()).isEqualTo(2020);
        assertThat(response.getBody().result().errors()).singleElement()
                .satisfies(error -> {
                    assertThat(error.fieldName()).isEqualTo("projectName");
                    assertThat(error.rejectValue()).isEqualTo("");
                    assertThat(error.message()).isEqualTo("Project name is required.");
                });
    }

    @Test
    void unreadableMessageReturnsSpecValidationFailure() {
        var response = handler.handleUnreadableMessage(new HttpMessageNotReadableException("bad json"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isFalse();
        assertThat(response.getBody().responseCode()).isEqualTo(2020);
        assertThat(response.getBody().result()).isNull();
    }

    private void assertFailure(String message, int responseCode) {
        var response = handler.handleIllegalArgument(new IllegalArgumentException(message));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().isSuccess()).isFalse();
        assertThat(response.getBody().responseCode()).isEqualTo(responseCode);
        assertThat(response.getBody().responseMessage()).isNotBlank();
    }

    private void assertFailure(String message, int responseCode, String responseMessage) {
        var response = handler.handleIllegalArgument(new IllegalArgumentException(message));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().responseCode()).isEqualTo(responseCode);
        assertThat(response.getBody().responseMessage()).isEqualTo(responseMessage);
    }

    private MethodArgumentNotValidException validationException(FieldError... errors) throws Exception {
        var target = new ValidationTarget("");
        var bindingResult = new BeanPropertyBindingResult(target, "request");
        for (var error : errors) {
            bindingResult.addError(error);
        }
        Method method = MobileSpecExceptionHandlerTest.class.getDeclaredMethod("validate", ValidationTarget.class);
        return new MethodArgumentNotValidException(new MethodParameter(method, 0), bindingResult);
    }

    @SuppressWarnings("unused")
    private void validate(@Valid ValidationTarget target) {
    }

    private record ValidationTarget(@NotBlank String title) {
    }
}
