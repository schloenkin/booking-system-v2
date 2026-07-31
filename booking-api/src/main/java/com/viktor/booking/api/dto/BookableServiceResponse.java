package com.viktor.booking.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(
        name = "BookableServiceResponse",
        description = "Bookable service returned by the API"
)
public class BookableServiceResponse {

    @Schema(
            description = "Unique service identifier",
            example = "10",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private Long id;

    @Schema(
            description = "Display name of the service",
            example = "Architecture consultation",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private String name;

    @Schema(
            description = "Detailed service description",
            example = "One-hour consultation with a software architect"
    )
    private String description;

    @Schema(
            description = "Service duration in minutes",
            example = "60",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private int durationMinutes;

    @Schema(
            description = "Whether the service currently accepts bookings",
            example = "true",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    private boolean active;

    public BookableServiceResponse(
            Long id,
            String name,
            String description,
            int durationMinutes,
            boolean active
    ) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.durationMinutes = durationMinutes;
        this.active = active;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public int getDurationMinutes() {
        return durationMinutes;
    }

    public boolean isActive() {
        return active;
    }
}
