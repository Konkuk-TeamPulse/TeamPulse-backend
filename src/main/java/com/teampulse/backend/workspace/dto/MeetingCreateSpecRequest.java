package com.teampulse.backend.workspace.dto;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;

public record MeetingCreateSpecRequest(
        @NotBlank(message = "Meeting title is required.")
        @Size(max = 120, message = "Meeting title must be 120 characters or fewer.")
        String title,

        @NotBlank(message = "Meeting date is required.")
        @Pattern(regexp = "\\d{4}-\\d{2}-\\d{2}", message = "Meeting date must use yyyy-MM-dd.")
        String meetingDate,

        @NotBlank(message = "Meeting agenda is required.")
        @Size(max = 500, message = "Meeting agenda must be 500 characters or fewer.")
        String agenda,

        @Size(max = 2000, message = "Meeting content must be 2000 characters or fewer.")
        String content,

        @JsonDeserialize(using = FlexibleStringListDeserializer.class)
        List<@Size(max = 2000, message = "Meeting decisions must be 2000 characters or fewer.") String> decisions,

        List<Long> attendeeIds,

        @Valid
        List<ActionItemRequest> actionItems
) {
    public record ActionItemRequest(
            @NotBlank(message = "Action item content is required.")
            @Size(max = 200, message = "Action item content must be 200 characters or fewer.")
            String content,
            Long assigneeId,
            @Pattern(regexp = "^$|\\d{4}-\\d{2}-\\d{2}", message = "Action item due date must use yyyy-MM-dd.")
            String dueDate
    ) {
    }
}
