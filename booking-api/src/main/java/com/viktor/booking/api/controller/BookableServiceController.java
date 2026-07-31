package com.viktor.booking.api.controller;

import com.viktor.booking.api.config.OpenApiConfig;
import com.viktor.booking.api.dto.BookableServiceCreateRequest;
import com.viktor.booking.api.dto.BookableServiceResponse;
import com.viktor.booking.api.dto.ErrorResponse;
import com.viktor.booking.application.exception.BookableServiceNotFoundException;
import com.viktor.booking.application.service.BookableServiceService;
import com.viktor.booking.domain.model.BookableService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@Tag(
        name = "Bookable Services",
        description = "Operations for viewing and administering bookable services"
)
public class BookableServiceController {

    private final BookableServiceService serviceService;

    public BookableServiceController(
            BookableServiceService serviceService
    ) {
        this.serviceService = serviceService;
    }

    @Operation(
            summary = "Create a bookable service",
            description = "Creates a new active bookable service. ADMIN role is required.",
            security = @SecurityRequirement(
                    name = OpenApiConfig.SECURITY_SCHEME_NAME
            )
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "Service created successfully",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(
                                    implementation = BookableServiceResponse.class
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Request validation failed",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(
                                    implementation = ErrorResponse.class
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Authentication is required",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(
                                    implementation = ErrorResponse.class
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "ADMIN role is required",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(
                                    implementation = ErrorResponse.class
                            )
                    )
            )
    })
    @PostMapping("/api/services")
    public ResponseEntity<BookableServiceResponse> createService(
            @Valid @RequestBody BookableServiceCreateRequest request
    ) {
        BookableService createdService =
                serviceService.createService(
                        request.getName(),
                        request.getDescription(),
                        request.getDurationMinutes()
                );

        return ResponseEntity
                .status(201)
                .body(toResponse(createdService));
    }

    @Operation(
            summary = "List all bookable services",
            description = "Returns all bookable services. Authentication is not required."
    )
    @ApiResponse(
            responseCode = "200",
            description = "Services returned successfully",
            content = @Content(
                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                    array = @ArraySchema(
                            schema = @Schema(
                                    implementation = BookableServiceResponse.class
                            )
                    )
            )
    )
    @GetMapping("/api/services")
    public List<BookableServiceResponse> getAllServices() {
        return serviceService.getAllServices()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Operation(
            summary = "Get a bookable service",
            description = "Returns one bookable service by its identifier."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Service returned successfully",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(
                                    implementation = BookableServiceResponse.class
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Service was not found",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(
                                    implementation = ErrorResponse.class
                            )
                    )
            )
    })
    @GetMapping("/api/services/{id}")
    public ResponseEntity<BookableServiceResponse> getServiceById(
            @Parameter(
                    description = "Service identifier",
                    example = "10",
                    required = true
            )
            @PathVariable("id")
            Long id
    ) {
        BookableService service =
                serviceService.getServiceById(id)
                        .orElseThrow(
                                () -> new BookableServiceNotFoundException(id)
                        );

        return ResponseEntity.ok(
                toResponse(service)
        );
    }

    @Operation(
            summary = "Activate a bookable service",
            description = "Marks a service as active. ADMIN role is required.",
            security = @SecurityRequirement(
                    name = OpenApiConfig.SECURITY_SCHEME_NAME
            )
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Service activated successfully",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(
                                    implementation = BookableServiceResponse.class
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Authentication is required",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(
                                    implementation = ErrorResponse.class
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "ADMIN role is required",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(
                                    implementation = ErrorResponse.class
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Service was not found",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(
                                    implementation = ErrorResponse.class
                            )
                    )
            )
    })
    @PutMapping("/api/services/{id}/activate")
    public ResponseEntity<BookableServiceResponse> activateServiceById(
            @Parameter(
                    description = "Service identifier",
                    example = "10",
                    required = true
            )
            @PathVariable("id")
            Long id
    ) {
        BookableService service =
                serviceService.activateServiceById(id)
                        .orElseThrow(
                                () -> new BookableServiceNotFoundException(id)
                        );

        return ResponseEntity.ok(
                toResponse(service)
        );
    }

    @Operation(
            summary = "Deactivate a bookable service",
            description = "Marks a service as inactive. ADMIN role is required.",
            security = @SecurityRequirement(
                    name = OpenApiConfig.SECURITY_SCHEME_NAME
            )
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Service deactivated successfully",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(
                                    implementation = BookableServiceResponse.class
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Authentication is required",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(
                                    implementation = ErrorResponse.class
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "ADMIN role is required",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(
                                    implementation = ErrorResponse.class
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Service was not found",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(
                                    implementation = ErrorResponse.class
                            )
                    )
            )
    })
    @PutMapping("/api/services/{id}/deactivate")
    public ResponseEntity<BookableServiceResponse> deactivateServiceById(
            @Parameter(
                    description = "Service identifier",
                    example = "10",
                    required = true
            )
            @PathVariable("id")
            Long id
    ) {
        BookableService service =
                serviceService.deactivateServiceById(id)
                        .orElseThrow(
                                () -> new BookableServiceNotFoundException(id)
                        );

        return ResponseEntity.ok(
                toResponse(service)
        );
    }

    private BookableServiceResponse toResponse(
            BookableService service
    ) {
        return new BookableServiceResponse(
                service.getId(),
                service.getName(),
                service.getDescription(),
                service.getDurationMinutes(),
                service.isActive()
        );
    }
}
