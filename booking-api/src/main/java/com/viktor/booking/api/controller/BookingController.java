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

import java.time.LocalDateTime;
import java.util.List;

@RestController
public class BookingController {

    private final BookingAuthorizationService authorizationService;

    public BookingController(
            BookingAuthorizationService authorizationService
    ) {
        this.authorizationService = authorizationService;
    }

    @GetMapping("/api/bookings")
    public PageResponse<BookingResponse> searchBookings(
            @AuthenticationPrincipal
            AuthenticatedUser authenticatedUser,

            @RequestParam(
                    name = "status",
                    required = false
            )
            BookingStatus status,

            @RequestParam(
                    name = "userId",
                    required = false
            )
            Long userId,

            @RequestParam(
                    name = "serviceId",
                    required = false
            )
            Long serviceId,

            @RequestParam(
                    name = "from",
                    required = false
            )
            @DateTimeFormat(
                    iso = DateTimeFormat.ISO.DATE_TIME
            )
            LocalDateTime from,

            @RequestParam(
                    name = "to",
                    required = false
            )
            @DateTimeFormat(
                    iso = DateTimeFormat.ISO.DATE_TIME
            )
            LocalDateTime to,

            @RequestParam(
                    name = "page",
                    defaultValue = "0"
            )
            int page,

            @RequestParam(
                    name = "size",
                    defaultValue = "20"
            )
            int size,

            @RequestParam(
                    name = "sortBy",
                    defaultValue = "startTime"
            )
            String sortBy,

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

    @GetMapping("/api/bookings/{id}")
    public ResponseEntity<BookingResponse> getBookingById(
            @AuthenticationPrincipal
            AuthenticatedUser authenticatedUser,

            @PathVariable("id")
            Long id
    ) {
        AuthenticatedUserContext context =
                toContext(authenticatedUser);

        return authorizationService
                .getBookingById(
                        context,
                        id
                )
                .map(this::toResponse)
                .map(ResponseEntity::ok)
                .orElse(
                        ResponseEntity
                                .notFound()
                                .build()
                );
    }

    @DeleteMapping("/api/bookings/{id}")
    public ResponseEntity<Void> deleteBookingById(
            @AuthenticationPrincipal
            AuthenticatedUser authenticatedUser,

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

        if (deleted) {
            return ResponseEntity
                    .noContent()
                    .build();
        }

        return ResponseEntity
                .notFound()
                .build();
    }

    @PutMapping("/api/bookings/{id}/cancel")
    public ResponseEntity<BookingResponse> cancelBookingById(
            @AuthenticationPrincipal
            AuthenticatedUser authenticatedUser,

            @PathVariable("id")
            Long id
    ) {
        AuthenticatedUserContext context =
                toContext(authenticatedUser);

        return authorizationService
                .cancelBookingById(
                        context,
                        id
                )
                .map(this::toResponse)
                .map(ResponseEntity::ok)
                .orElse(
                        ResponseEntity
                                .notFound()
                                .build()
                );
    }

    @PutMapping("/api/bookings/{id}/confirm")
    public ResponseEntity<BookingResponse> confirmBookingById(
            @AuthenticationPrincipal
            AuthenticatedUser authenticatedUser,

            @PathVariable("id")
            Long id
    ) {
        AuthenticatedUserContext context =
                toContext(authenticatedUser);

        return authorizationService
                .confirmBookingById(
                        context,
                        id
                )
                .map(this::toResponse)
                .map(ResponseEntity::ok)
                .orElse(
                        ResponseEntity
                                .notFound()
                                .build()
                );
    }

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
