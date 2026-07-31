package com.viktor.booking.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(
        name = "BookingResponse",
        description = "Booking returned by the API"
)
public class BookingResponse {

    @Schema(
            description = "Unique booking identifier",
            example = "25",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private Long id;

    @Schema(
            description = "Identifier of the user who owns the booking",
            example = "7",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private Long userId;

    @Schema(
            description = "Identifier of the booked service",
            example = "10",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private Long serviceId;

    @Schema(
            description = "Booking start time",
            example = "2035-08-10T10:00:00",
            type = "string",
            format = "date-time",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private LocalDateTime startTime;

    @Schema(
            description = "Booking end time",
            example = "2035-08-10T11:00:00",
            type = "string",
            format = "date-time",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private LocalDateTime endTime;

    @Schema(
            description = "Current booking status",
            example = "PENDING",
            allowableValues = {
                    "PENDING",
                    "CONFIRMED",
                    "CANCELLED"
            },
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private String status;

    public BookingResponse(
            Long id,
            Long userId,
            Long serviceId,
            LocalDateTime startTime,
            LocalDateTime endTime,
            String status
    ) {
        this.id = id;
        this.userId = userId;
        this.serviceId = serviceId;
        this.startTime = startTime;
        this.endTime = endTime;
        this.status = status;
    }

    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public Long getServiceId() {
        return serviceId;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public String getStatus() {
        return status;
    }
}