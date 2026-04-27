package com.teampulse.backend.mobile.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;

public record CreateMeetingRequest(
        @NotBlank(message = "Meeting title is required.")
        @Size(max = 120, message = "Meeting title must be 120 characters or fewer.")
        String title,
        @NotBlank(message = "Meeting time is required.")
        @Pattern(
                regexp = "\\d{4}-\\d{2}-\\d{2}(T\\d{2}:\\d{2})?",
                message = "Meeting time must use yyyy-MM-dd or yyyy-MM-ddTHH:mm."
        )
        String time,
        @NotBlank(message = "Meeting agenda is required.")
        @Size(max = 500, message = "Meeting agenda must be 500 characters or fewer.")
        String agenda,
        List<String> decisions,
        List<String> actions,
        String actionOwner,
        boolean createTasks
) {
}
