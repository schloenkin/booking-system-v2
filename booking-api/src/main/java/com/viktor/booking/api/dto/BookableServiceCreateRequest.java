package com.viktor.booking.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

@Schema(
        name = "BookableServiceCreateRequest",
        description = "Data required to create a bookable service"
)
public class BookableServiceCreateRequest {

    @Schema(
            description = "Display name of the service",
            example = "Architecture consultation",
            maxLength = 150,
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotBlank(message = "Name must not be blank")
    @Size(
            max = 150,
            message = "Name must not contain more than 150 characters"
    )
    private String name;

    @Schema(
            description = "Detailed description of the service",
            example = "One-hour consultation with a software architect",
            maxLength = 2000
    )
    @Size(
            max = 2000,
            message = "Description must not contain more than 2000 characters"
    )
    private String description;

    @Schema(
            description = "Service duration in minutes",
            example = "60",
            minimum = "1",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    @Positive(message = "Duration must be greater than zero")
    private int durationMinutes;

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public int getDurationMinutes() {
        return durationMinutes;
    }
}
