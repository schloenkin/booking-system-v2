package com.viktor.booking.api.controller;

import com.viktor.booking.api.dto.BookingCreateRequest;
import com.viktor.booking.api.dto.BookingResponse;
import com.viktor.booking.api.dto.PageResponse;
import com.viktor.booking.api.security.AuthenticatedUser;
import com.viktor.booking.application.query.BookingSearchCriteria;
import com.viktor.booking.application.query.PageRequestData;
import com.viktor.booking.application.query.PageResult;
import com.viktor.booking.application.security.AuthenticatedUserContext;
import com.viktor.booking.application.service.BookingAuthorizationService;
import com.viktor.booking.domain.enums.BookingStatus;
import com.viktor.booking.domain.model.Booking;
import com.viktor.booking.api.config.OpenApiConfig;
import com.viktor.booking.api.dto.ErrorResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.http.MediaType;

import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;


import java.time.LocalDateTime;
import java.util.List;

@RestController
@Tag(
        name = "Bookings",
        description = """
                Operations for creating, searching and managing bookings
                """
)
@SecurityRequirement(
        name = OpenApiConfig.SECURITY_SCHEME_NAME
)
public class BookingController {

    private final BookingAuthorizationService authorizationService;

    public BookingController(
            BookingAuthorizationService authorizationService
    ) {
        this.authorizationService = authorizationService;
    }

    @Operation(
            summary = "Search bookings",
            description = """
                Returns a paginated list of bookings using optional filters, \
                sorting and pagination.

                USER accounts can access only their own bookings.
                ADMIN accounts can search all bookings.
                """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Bookings returned successfully",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(
                                    implementation = PageResponse.class
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Search, pagination or sorting parameters are invalid",
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
            )
    })
    @GetMapping("/api/bookings")
    public PageResponse<BookingResponse> searchBookings(
            @AuthenticationPrincipal
            AuthenticatedUser authenticatedUser,

            @Parameter(
                    description = "Filter bookings by status",
                    example = "PENDING",
                    schema = @Schema(
                            allowableValues = {
                                    "PENDING",
                                    "CONFIRMED",
                                    "CANCELLED"
                            }
                    )
            )
            @RequestParam(
                    name = "status",
                    required = false
            )
            BookingStatus status,

            @Parameter(
                    description = """
                        Filter by booking owner. ADMIN accounts can search \
                        by any user identifier. USER accounts are restricted \
                        to their own bookings.
                        """,
                    example = "7"
            )
            @RequestParam(
                    name = "userId",
                    required = false
            )
            Long userId,

            @Parameter(
                    description = "Filter by bookable service identifier",
                    example = "10"
            )
            @RequestParam(
                    name = "serviceId",
                    required = false
            )
            Long serviceId,

            @Parameter(
                    description = "Include bookings starting at or after this date and time",
                    example = "2035-08-01T00:00:00"
            )
            @RequestParam(
                    name = "from",
                    required = false
            )
            @DateTimeFormat(
                    iso = DateTimeFormat.ISO.DATE_TIME
            )
            LocalDateTime from,

            @Parameter(
                    description = "Include bookings ending at or before this date and time",
                    example = "2035-08-31T23:59:59"
            )
            @RequestParam(
                    name = "to",
                    required = false
            )
            @DateTimeFormat(
                    iso = DateTimeFormat.ISO.DATE_TIME
            )
            LocalDateTime to,

            @Parameter(
                    description = "Zero-based page number",
                    example = "0"
            )
            @RequestParam(
                    name = "page",
                    defaultValue = "0"
            )
            int page,

            @Parameter(
                    description = "Maximum number of bookings returned per page",
                    example = "20"
            )
            @RequestParam(
                    name = "size",
                    defaultValue = "20"
            )
            int size,

            @Parameter(
                    description = "Booking property used for sorting",
                    example = "startTime"
            )
            @RequestParam(
                    name = "sortBy",
                    defaultValue = "startTime"
            )
            String sortBy,

            @Parameter(
                    description = "Sorting direction",
                    example = "asc",
                    schema = @Schema(
                            allowableValues = {
                                    "asc",
                                    "desc"
                            }
                    )
            )
            @RequestParam(
                    name = "direction",
                    defaultValue = "asc"
            )
            String direction
    ) {
        AuthenticatedUserContext context =
                toContext(authenticatedUser);

        BookingSearchCriteria requestedCriteria =
                new BookingSearchCriteria(
                        status,
                        userId,
                        serviceId,
                        from,
                        to
                );

        PageRequestData pageRequest =
                new PageRequestData(
                        page,
                        size,
                        sortBy,
                        direction
                );

        PageResult<Booking> result =
                authorizationService.searchBookings(
                        context,
                        requestedCriteria,
                        pageRequest
                );

        return toPageResponse(result);
    }

    @Operation(
            summary = "Get a booking",
            description = """
                Returns a booking by its identifier.

                USER accounts can access only their own bookings.
                ADMIN accounts can access any booking.
                A booking that is absent or inaccessible is returned as not found.
                """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Booking returned successfully",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(
                                    implementation = BookingResponse.class
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
                    responseCode = "404",
                    description = "Booking was not found or is not accessible",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(
                                    implementation = ErrorResponse.class
                            )
                    )
            )
    })
    @GetMapping("/api/bookings/{id}")
    public ResponseEntity<BookingResponse> getBookingById(
            @AuthenticationPrincipal
            AuthenticatedUser authenticatedUser,

            @Parameter(
                    description = "Booking identifier",
                    example = "25",
                    required = true
            )
            @PathVariable("id")
            Long id
    ) {
        AuthenticatedUserContext context =
                toContext(authenticatedUser);

        Booking booking =
                authorizationService
                        .getBookingById(
                                context,
                                id
                        )
                        .orElseThrow(
                                () -> new ResponseStatusException(
                                        HttpStatus.NOT_FOUND,
                                        "Booking not found"
                                )
                        );

        return ResponseEntity.ok(
                toResponse(booking)
        );
    }

    @Operation(
            summary = "Delete a booking",
            description = """
                Permanently deletes an existing booking.

                Only ADMIN accounts can delete bookings.
                The booking must be in a status that permits deletion.
                """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "204",
                    description = "Booking deleted successfully",
                    content = @Content
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
                    description = "Booking was not found",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(
                                    implementation = ErrorResponse.class
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Booking cannot be deleted in its current status",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(
                                    implementation = ErrorResponse.class
                            )
                    )
            )
    })
    @DeleteMapping("/api/bookings/{id}")
    public ResponseEntity<Void> deleteBookingById(
            @AuthenticationPrincipal
            AuthenticatedUser authenticatedUser,

            @Parameter(
                    description = "Booking identifier",
                    example = "25",
                    required = true
            )
            @PathVariable("id")
            Long id
    ) {
        AuthenticatedUserContext context =
                toContext(authenticatedUser);

        boolean deleted =
                authorizationService.deleteBookingById(
                        context,
                        id
                );

        if (!deleted) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Booking not found"
            );
        }

        return ResponseEntity
                .noContent()
                .build();
    }

    @Operation(
            summary = "Cancel a booking",
            description = """
                Cancels an existing booking.

                USER accounts can cancel only their own bookings.
                ADMIN accounts can cancel any booking.
                A booking that is absent or inaccessible is returned as not found.
                """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Booking cancelled successfully",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(
                                    implementation = BookingResponse.class
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
                    responseCode = "404",
                    description = "Booking was not found or is not accessible",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(
                                    implementation = ErrorResponse.class
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Booking cannot be cancelled in its current status",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(
                                    implementation = ErrorResponse.class
                            )
                    )
            )
    })
    @PutMapping("/api/bookings/{id}/cancel")
    public ResponseEntity<BookingResponse> cancelBookingById(
            @AuthenticationPrincipal
            AuthenticatedUser authenticatedUser,

            @Parameter(
                    description = "Booking identifier",
                    example = "25",
                    required = true
            )
            @PathVariable("id")
            Long id
    ) {
        AuthenticatedUserContext context =
                toContext(authenticatedUser);

        Booking booking =
                authorizationService
                        .cancelBookingById(
                                context,
                                id
                        )
                        .orElseThrow(
                                () -> new ResponseStatusException(
                                        HttpStatus.NOT_FOUND,
                                        "Booking not found"
                                )
                        );

        return ResponseEntity.ok(
                toResponse(booking)
        );
    }

    @Operation(
            summary = "Confirm a booking",
            description = """
                Confirms an existing booking.

                Only ADMIN accounts can confirm bookings.
                A booking that does not exist is returned as not found.
                """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Booking confirmed successfully",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(
                                    implementation = BookingResponse.class
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
                    description = "Booking was not found",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(
                                    implementation = ErrorResponse.class
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Booking cannot be confirmed in its current status",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(
                                    implementation = ErrorResponse.class
                            )
                    )
            )
    })
    @PutMapping("/api/bookings/{id}/confirm")
    public ResponseEntity<BookingResponse> confirmBookingById(
            @AuthenticationPrincipal
            AuthenticatedUser authenticatedUser,

            @Parameter(
                    description = "Booking identifier",
                    example = "25",
                    required = true
            )
            @PathVariable("id")
            Long id
    ) {
        AuthenticatedUserContext context =
                toContext(authenticatedUser);

        Booking booking =
                authorizationService
                        .confirmBookingById(
                                context,
                                id
                        )
                        .orElseThrow(
                                () -> new ResponseStatusException(
                                        HttpStatus.NOT_FOUND,
                                        "Booking not found"
                                )
                        );

        return ResponseEntity.ok(
                toResponse(booking)
        );
    }

    @Operation(
            summary = "Create a booking",
            description = """
                Creates a new booking for the authenticated user.

                The selected service must exist and be active.
                The requested time interval must be valid and must not \
                conflict with an existing booking.
                """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "Booking created successfully",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(
                                    implementation = BookingResponse.class
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Request validation or booking time interval is invalid",
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
                    responseCode = "404",
                    description = "Bookable service was not found",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(
                                    implementation = ErrorResponse.class
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = """
                        Booking conflicts with an existing booking or \
                        the selected service cannot currently be booked
                        """,
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(
                                    implementation = ErrorResponse.class
                            )
                    )
            )
    })
    @PostMapping("/api/bookings")
    public ResponseEntity<BookingResponse> createBooking(
            @AuthenticationPrincipal
            AuthenticatedUser authenticatedUser,

            @Valid
            @RequestBody
            BookingCreateRequest request
    ) {
        AuthenticatedUserContext context =
                toContext(authenticatedUser);

        Booking booking =
                authorizationService.createBooking(
                        context,
                        request.getServiceId(),
                        request.getStartTime(),
                        request.getEndTime()
                );

        return ResponseEntity
                .status(201)
                .body(toResponse(booking));
    }
    private AuthenticatedUserContext toContext(
            AuthenticatedUser authenticatedUser
    ) {
        return new AuthenticatedUserContext(
                authenticatedUser.getUserId(),
                authenticatedUser.getRole()
        );
    }

    private PageResponse<BookingResponse> toPageResponse(
            PageResult<Booking> result
    ) {
        List<BookingResponse> content =
                result.content()
                        .stream()
                        .map(this::toResponse)
                        .toList();

        return new PageResponse<>(
                content,
                result.page(),
                result.size(),
                result.totalElements(),
                result.totalPages(),
                result.first(),
                result.last()
        );
    }

    private BookingResponse toResponse(
            Booking booking
    ) {
        return new BookingResponse(
                booking.getId(),
                booking.getUserId(),
                booking.getServiceId(),
                booking.getStartTime(),
                booking.getEndTime(),
                booking.getStatus().name()
        );
    }
}
