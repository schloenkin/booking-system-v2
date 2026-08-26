package com.viktor.booking.api.dto;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
@Schema(
        name = "BookingRescheduleRequest",
        description = "A new time required to reschedule an existing booking"
)
public class BookingRescheduleRequest {
    @Schema(
            description = "New booking start time",
            example = "2035-08-10T11:00:00",
            type = "string",
            format = "date-time",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotNull(message = "Start time must not be null")
    private LocalDateTime startTime;

    @Schema(
            description = "New booking end time",
            example = "2035-08-10T12:00:00",
            type = "string",
            format = "date-time",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotNull(message = "End time must not be null")
    private LocalDateTime endTime;

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

}
