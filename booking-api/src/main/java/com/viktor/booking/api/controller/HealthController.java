package com.viktor.booking.api.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(
        name = "Health",
        description = "Application availability check"
)
public class HealthController {

    @Operation(
            summary = "Check API health",
            description = """
                    Returns a simple message confirming that the Booking API \
                    application is running.
                    """
    )
    @ApiResponse(
            responseCode = "200",
            description = "API is running",
            content = @Content(
                    mediaType = MediaType.TEXT_PLAIN_VALUE,
                    schema = @Schema(
                            type = "string",
                            example = "Booking API is running"
                    )
            )
    )
    @GetMapping("/api/health")
    public String health() {
        return "Booking API is running";
    }
}
