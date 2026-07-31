package com.viktor.booking.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

@Schema(
        name = "BookingCreateRequest",
        description = "Data required to create a new booking"
)
public class BookingCreateRequest {

    @Schema(
            description = "Identifier of the service being booked",
            example = "10",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotNull(message = "Service id must not be null")
    private Long serviceId;

    @Schema(
            description = "Requested booking start time",
            example = "2035-08-10T10:00:00",
            type = "string",
            format = "date-time",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotNull(message = "Start time must not be null")
    private LocalDateTime startTime;

    @Schema(
            description = "Requested booking end time",
            example = "2035-08-10T11:00:00",
            type = "string",
            format = "date-time",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotNull(message = "End time must not be null")
    private LocalDateTime endTime;

    public Long getServiceId() {
        return serviceId;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }
}