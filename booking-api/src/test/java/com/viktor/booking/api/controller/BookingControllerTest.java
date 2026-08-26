package com.viktor.booking.api.controller;

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.viktor.booking.api.exception.GlobalExceptionHandler;
import com.viktor.booking.api.security.AuthenticatedUser;
import com.viktor.booking.application.exception.BookingTimeConflictException;
import com.viktor.booking.application.exception.InvalidBookingSearchException;
import com.viktor.booking.application.query.BookingSearchCriteria;
import com.viktor.booking.application.query.PageRequestData;
import com.viktor.booking.application.query.PageResult;
import com.viktor.booking.application.security.AuthenticatedUserContext;
import com.viktor.booking.application.service.BookingAuthorizationService;
import com.viktor.booking.domain.enums.BookingStatus;
import com.viktor.booking.domain.enums.UserRole;
import com.viktor.booking.application.exception.BookingOperationForbiddenException;
import com.viktor.booking.domain.exception.BookingCannotBeDeletedException;
import com.viktor.booking.domain.exception.BookingCannotBeConfirmedException;
import com.viktor.booking.domain.exception.BookingCannotBeRescheduledException;
import com.viktor.booking.domain.model.Booking;
import com.viktor.booking.domain.model.User;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class BookingControllerTest {

    @Mock
    private BookingAuthorizationService authorizationService;

    private MockMvc mockMvc;
    private AuthenticatedUserContext userContext;

    @BeforeEach
    void setUp() {
        authenticateAs(
                1L,
                UserRole.USER
        );

        BookingController bookingController =
                new BookingController(
                        authorizationService
                );

        ObjectMapper objectMapper =
                new ObjectMapper();

        objectMapper.registerModule(
                new JavaTimeModule()
        );

        objectMapper.disable(
                SerializationFeature.WRITE_DATES_AS_TIMESTAMPS
        );

        mockMvc = MockMvcBuilders
                .standaloneSetup(bookingController)
                .setCustomArgumentResolvers(
                        new AuthenticationPrincipalArgumentResolver()
                )
                .setControllerAdvice(
                        new GlobalExceptionHandler()
                )
                .setMessageConverters(
                        new MappingJackson2HttpMessageConverter(
                                objectMapper
                        )
                )
                .build();
    }

    private void authenticateAs(
            Long userId,
            UserRole role
    ) {
        User user = new User(
                userId,
                "user-" + userId + "@example.com",
                "hashed-password",
                role
        );

        AuthenticatedUser authenticatedUser =
                new AuthenticatedUser(user);

        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(
                        authenticatedUser,
                        null,
                        authenticatedUser.getAuthorities()
                );

        SecurityContextHolder
                .getContext()
                .setAuthentication(authentication);

        userContext =
                new AuthenticatedUserContext(
                        userId,
                        role
                );
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldReturnConfirmedBookingForAdmin()
            throws Exception {
        authenticateAs(
                100L,
                UserRole.ADMIN
        );

        Long bookingId = 8L;

        LocalDateTime startTime =
                LocalDateTime.now().plusDays(1);

        Booking confirmedBooking =
                Booking.restore(
                        bookingId,
                        1L,
                        2L,
                        startTime,
                        startTime.plusMinutes(60),
                        BookingStatus.CONFIRMED
                );

        when(authorizationService.confirmBookingById(
                userContext,
                bookingId
        )).thenReturn(
                Optional.of(confirmedBooking)
        );

        mockMvc.perform(
                        put(
                                "/api/bookings/{id}/confirm",
                                bookingId
                        )
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(8))
                .andExpect(jsonPath("$.userId").value(1))
                .andExpect(jsonPath("$.serviceId").value(2))
                .andExpect(
                        jsonPath("$.status")
                                .value("CONFIRMED")
                );

        verify(authorizationService)
                .confirmBookingById(
                        userContext,
                        bookingId
                );
    }

    @Test
    void shouldReturn404WhenBookingToConfirmIsNotAccessible()
            throws Exception {

        Long bookingId = 999999L;

        when(authorizationService.confirmBookingById(
                userContext,
                bookingId
        )).thenReturn(Optional.empty());

        mockMvc.perform(
                        put(
                                "/api/bookings/{id}/confirm",
                                bookingId
                        )
                )
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturn403WhenUserTriesToConfirmBooking()
            throws Exception {

        Long bookingId = 8L;

        when(authorizationService.confirmBookingById(
                userContext,
                bookingId
        )).thenThrow(
                new BookingOperationForbiddenException(
                        "confirm"
                )
        );

        mockMvc.perform(
                        put(
                                "/api/bookings/{id}/confirm",
                                bookingId
                        )
                )
                .andExpect(status().isForbidden())
                .andExpect(
                        jsonPath("$.message")
                                .value(
                                        "Current user is not allowed "
                                                + "to confirm booking"
                                )
                );

        verify(authorizationService)
                .confirmBookingById(
                        userContext,
                        bookingId
                );
    }

    @Test
    void shouldReturn409WhenBookingCannotBeConfirmed()
            throws Exception {

        Long bookingId = 9L;

        when(authorizationService.confirmBookingById(
                userContext,
                bookingId
        )).thenThrow(
                new BookingCannotBeConfirmedException(
                        BookingStatus.CANCELLED
                )
        );

        mockMvc.perform(
                        put(
                                "/api/bookings/{id}/confirm",
                                bookingId
                        )
                )
                .andExpect(status().isConflict())
                .andExpect(
                        jsonPath("$.message")
                                .value(
                                        "Booking cannot be confirmed "
                                                + "from status: CANCELLED"
                                )
                );
    }

    @Test
    void shouldReturnFirstPageOfBookings()
            throws Exception {

        LocalDateTime startTime =
                LocalDateTime.now().plusDays(1);

        Booking firstBooking =
                Booking.restore(
                        1L,
                        1L,
                        2L,
                        startTime,
                        startTime.plusMinutes(60),
                        BookingStatus.PENDING
                );

        Booking secondBooking =
                Booking.restore(
                        2L,
                        1L,
                        4L,
                        startTime.plusHours(2),
                        startTime.plusHours(3),
                        BookingStatus.CONFIRMED
                );

        BookingSearchCriteria criteria =
                new BookingSearchCriteria(
                        null,
                        null,
                        null,
                        null,
                        null
                );

        PageRequestData pageRequest =
                new PageRequestData(
                        0,
                        20,
                        "startTime",
                        "asc"
                );

        PageResult<Booking> pageResult =
                new PageResult<>(
                        List.of(
                                firstBooking,
                                secondBooking
                        ),
                        0,
                        20,
                        2,
                        1,
                        true,
                        true
                );

        when(authorizationService.searchBookings(
                userContext,
                criteria,
                pageRequest
        )).thenReturn(pageResult);

        mockMvc.perform(
                        get("/api/bookings")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(
                        jsonPath("$.content.length()")
                                .value(2)
                )
                .andExpect(
                        jsonPath("$.content[0].id")
                                .value(1)
                )
                .andExpect(
                        jsonPath("$.content[0].status")
                                .value("PENDING")
                )
                .andExpect(
                        jsonPath("$.content[1].id")
                                .value(2)
                )
                .andExpect(
                        jsonPath("$.content[1].status")
                                .value("CONFIRMED")
                )
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(
                        jsonPath("$.totalElements")
                                .value(2)
                )
                .andExpect(
                        jsonPath("$.totalPages")
                                .value(1)
                )
                .andExpect(jsonPath("$.first").value(true))
                .andExpect(jsonPath("$.last").value(true));

        verify(authorizationService)
                .searchBookings(
                        userContext,
                        criteria,
                        pageRequest
                );
    }

    @Test
    void shouldPassSearchParametersAndAuthenticatedContext()
            throws Exception {

        LocalDateTime startTime =
                LocalDateTime.of(
                        2030,
                        4,
                        15,
                        10,
                        0
                );

        Booking booking =
                Booking.restore(
                        10L,
                        1L,
                        7L,
                        startTime,
                        startTime.plusMinutes(60),
                        BookingStatus.PENDING
                );

        BookingSearchCriteria criteria =
                new BookingSearchCriteria(
                        BookingStatus.PENDING,
                        999L,
                        7L,
                        LocalDateTime.of(
                                2030,
                                4,
                                1,
                                0,
                                0
                        ),
                        LocalDateTime.of(
                                2030,
                                4,
                                30,
                                23,
                                59
                        )
                );

        PageRequestData pageRequest =
                new PageRequestData(
                        1,
                        5,
                        "endTime",
                        "desc"
                );

        PageResult<Booking> pageResult =
                new PageResult<>(
                        List.of(booking),
                        1,
                        5,
                        6,
                        2,
                        false,
                        true
                );

        when(authorizationService.searchBookings(
                userContext,
                criteria,
                pageRequest
        )).thenReturn(pageResult);

        mockMvc.perform(
                        get("/api/bookings")
                                .param("status", "PENDING")
                                .param("userId", "999")
                                .param("serviceId", "7")
                                .param(
                                        "from",
                                        "2030-04-01T00:00:00"
                                )
                                .param(
                                        "to",
                                        "2030-04-30T23:59:00"
                                )
                                .param("page", "1")
                                .param("size", "5")
                                .param("sortBy", "endTime")
                                .param("direction", "desc")
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.content.length()")
                                .value(1)
                )
                .andExpect(
                        jsonPath("$.content[0].id")
                                .value(10)
                )
                .andExpect(
                        jsonPath("$.content[0].userId")
                                .value(1)
                )
                .andExpect(
                        jsonPath("$.content[0].serviceId")
                                .value(7)
                )
                .andExpect(
                        jsonPath("$.content[0].status")
                                .value("PENDING")
                );

        verify(authorizationService)
                .searchBookings(
                        userContext,
                        criteria,
                        pageRequest
                );
    }

    @Test
    void shouldReturnAccessibleBookingById()
            throws Exception {

        Long bookingId = 3L;

        LocalDateTime startTime =
                LocalDateTime.now().plusDays(1);

        Booking booking =
                Booking.restore(
                        bookingId,
                        1L,
                        2L,
                        startTime,
                        startTime.plusMinutes(60),
                        BookingStatus.PENDING
                );

        when(authorizationService.getBookingById(
                userContext,
                bookingId
        )).thenReturn(Optional.of(booking));

        mockMvc.perform(
                        get(
                                "/api/bookings/{id}",
                                bookingId
                        )
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(3))
                .andExpect(jsonPath("$.userId").value(1))
                .andExpect(jsonPath("$.serviceId").value(2))
                .andExpect(
                        jsonPath("$.status")
                                .value("PENDING")
                );
    }

    @Test
    void shouldReturn404WhenBookingIsNotAccessible()
            throws Exception {

        Long bookingId = 999999L;

        when(authorizationService.getBookingById(
                userContext,
                bookingId
        )).thenReturn(Optional.empty());

        mockMvc.perform(
                        get(
                                "/api/bookings/{id}",
                                bookingId
                        )
                )
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldCreateBookingForAuthenticatedUser()
            throws Exception {

        LocalDateTime startTime =
                LocalDateTime.of(
                        2030,
                        1,
                        15,
                        10,
                        0
                );

        LocalDateTime endTime =
                startTime.plusMinutes(60);

        Booking createdBooking =
                Booking.restore(
                        10L,
                        1L,
                        2L,
                        startTime,
                        endTime,
                        BookingStatus.PENDING
                );

        when(authorizationService.createBooking(
                userContext,
                2L,
                startTime,
                endTime
        )).thenReturn(createdBooking);

        String requestBody = """
                {
                  "serviceId": 2,
                  "startTime": "2030-01-15T10:00:00",
                  "endTime": "2030-01-15T11:00:00"
                }
                """;

        mockMvc.perform(
                        post("/api/bookings")
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(requestBody)
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.userId").value(1))
                .andExpect(jsonPath("$.serviceId").value(2))
                .andExpect(
                        jsonPath("$.status")
                                .value("PENDING")
                );

        verify(authorizationService)
                .createBooking(
                        userContext,
                        2L,
                        startTime,
                        endTime
                );
    }

    @Test
    void shouldReturn400WhenCreateBookingRequestIsInvalid()
            throws Exception {

        String requestBody = """
                {
                  "startTime": "2030-01-15T10:00:00",
                  "endTime": "2030-01-15T11:00:00"
                }
                """;

        mockMvc.perform(
                        post("/api/bookings")
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(requestBody)
                )
                .andExpect(status().isBadRequest())
                .andExpect(
                        jsonPath("$.code")
                                .value("VALIDATION_FAILED")
                )
                .andExpect(
                        jsonPath("$.message")
                                .value("Request validation failed")
                )
                .andExpect(
                        jsonPath(
                                "$.violations[?(@.field == 'serviceId')].message"
                        ).value(
                                org.hamcrest.Matchers.hasItem(
                                        "Service id must not be null"
                                )
                        )
                );
    }

    @Test
    void shouldCancelAccessibleBooking()
            throws Exception {

        Long bookingId = 11L;

        LocalDateTime startTime =
                LocalDateTime.of(
                        2030,
                        1,
                        16,
                        10,
                        0
                );

        Booking cancelledBooking =
                Booking.restore(
                        bookingId,
                        1L,
                        2L,
                        startTime,
                        startTime.plusMinutes(60),
                        BookingStatus.CANCELLED
                );

        when(authorizationService.cancelBookingById(
                userContext,
                bookingId
        )).thenReturn(
                Optional.of(cancelledBooking)
        );

        mockMvc.perform(
                        put(
                                "/api/bookings/{id}/cancel",
                                bookingId
                        )
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(11))
                .andExpect(
                        jsonPath("$.status")
                                .value("CANCELLED")
                );
    }

    @Test
    void shouldReturn404WhenBookingToCancelIsNotAccessible()
            throws Exception {

        Long bookingId = 999999L;

        when(authorizationService.cancelBookingById(
                userContext,
                bookingId
        )).thenReturn(Optional.empty());

        mockMvc.perform(
                        put(
                                "/api/bookings/{id}/cancel",
                                bookingId
                        )
                )
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturn204WhenAdminDeletesBooking()
            throws Exception {
        authenticateAs(
                100L,
                UserRole.ADMIN
        );

        Long bookingId = 12L;

        when(authorizationService.deleteBookingById(
                userContext,
                bookingId
        )).thenReturn(true);

        mockMvc.perform(
                        delete(
                                "/api/bookings/{id}",
                                bookingId
                        )
                )
                .andExpect(status().isNoContent());

        verify(authorizationService)
                .deleteBookingById(
                        userContext,
                        bookingId
                );
    }

    @Test
    void shouldReturn403WhenUserTriesToDeleteBooking()
            throws Exception {

        Long bookingId = 12L;

        when(authorizationService.deleteBookingById(
                userContext,
                bookingId
        )).thenThrow(
                new BookingOperationForbiddenException(
                        "delete"
                )
        );

        mockMvc.perform(
                        delete(
                                "/api/bookings/{id}",
                                bookingId
                        )
                )
                .andExpect(status().isForbidden())
                .andExpect(
                        jsonPath("$.message")
                                .value(
                                        "Current user is not allowed "
                                                + "to delete booking"
                                )
                );

        verify(authorizationService)
                .deleteBookingById(
                        userContext,
                        bookingId
                );
    }

    @Test
    void shouldReturn409WhenBookingCannotBeDeleted()
            throws Exception {

        authenticateAs(
                100L,
                UserRole.ADMIN
        );

        Long bookingId = 12L;

        when(authorizationService.deleteBookingById(
                userContext,
                bookingId
        )).thenThrow(
                new BookingCannotBeDeletedException(
                        BookingStatus.CONFIRMED
                )
        );

        mockMvc.perform(
                        delete(
                                "/api/bookings/{id}",
                                bookingId
                        )
                )
                .andExpect(status().isConflict())
                .andExpect(
                        jsonPath("$.message")
                                .value(
                                        "Booking cannot be deleted "
                                                + "from status: CONFIRMED"
                                )
                );

        verify(authorizationService)
                .deleteBookingById(
                        userContext,
                        bookingId
                );
    }

    @Test
    void shouldReturn404WhenBookingToDeleteIsNotAccessible()
            throws Exception {

        Long bookingId = 999999L;

        when(authorizationService.deleteBookingById(
                userContext,
                bookingId
        )).thenReturn(false);

        mockMvc.perform(
                        delete(
                                "/api/bookings/{id}",
                                bookingId
                        )
                )
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturnBadRequestWhenBookingSearchIsInvalid()
            throws Exception {

        BookingSearchCriteria criteria =
                new BookingSearchCriteria(
                        null,
                        null,
                        null,
                        null,
                        null
                );

        PageRequestData pageRequest =
                new PageRequestData(
                        -1,
                        20,
                        "startTime",
                        "asc"
                );

        when(authorizationService.searchBookings(
                userContext,
                criteria,
                pageRequest
        )).thenThrow(
                new InvalidBookingSearchException(
                        "Page number must not be negative"
                )
        );

        mockMvc.perform(
                        get("/api/bookings")
                                .param("page", "-1")
                )
                .andExpect(status().isBadRequest())
                .andExpect(
                        jsonPath("$.status")
                                .value(400)
                )
                .andExpect(
                        jsonPath("$.message")
                                .value(
                                        "Page number must not be negative"
                                )
                )
                .andExpect(
                        jsonPath("$.path")
                                .value("/api/bookings")
                );

        verify(authorizationService)
                .searchBookings(
                        userContext,
                        criteria,
                        pageRequest
                );
    }

    @Test
    void shouldRescheduleBooking()
            throws Exception {
        Long bookingId = 11L;

        LocalDateTime newStartTime = LocalDateTime.of(2030, 1, 15, 10, 0);
        LocalDateTime newEndTime = LocalDateTime.of(2030, 1, 15, 11, 0);


        Booking rescheduledBooking = Booking.restore(
                bookingId,
                1L,
                2L,
                newStartTime,
                newEndTime,
                BookingStatus.PENDING
        );
        when(authorizationService.rescheduleBookingById(
                userContext,
                bookingId,
                newStartTime,
                newEndTime)
        )
                .thenReturn(Optional.of(rescheduledBooking)
                );

        String requestBody = """
                {
                  "startTime": "2030-01-15T10:00:00",
                  "endTime": "2030-01-15T11:00:00"
                }
                """;

        mockMvc.perform(
                        put("/api/bookings/{id}/reschedule",
                                bookingId)
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(requestBody)
                ).andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(11))
                .andExpect(jsonPath("$.startTime").value("2030-01-15T10:00:00"))
                .andExpect(jsonPath("$.endTime").value("2030-01-15T11:00:00"))
                .andExpect(
                        jsonPath("$.status")
                                .value("PENDING")
                );
        verify(authorizationService)
                .rescheduleBookingById(
                        userContext,
                        bookingId,
                        newStartTime,
                        newEndTime
                );
    }

    @Test
    void shouldReturnNotFoundWhenBookingToRescheduleDoesNotExist()
            throws Exception {
        Long bookingId = 9999L;

        LocalDateTime newStartTime =
                LocalDateTime.of(2030, 1, 15, 10, 0);

        LocalDateTime newEndTime =
                LocalDateTime.of(2030, 1, 15, 11, 0);

        String requestBody = """
                {
                  "startTime": "2030-01-15T10:00:00",
                  "endTime": "2030-01-15T11:00:00"
                }
                """;

        when(authorizationService.rescheduleBookingById(
                userContext,
                bookingId,
                newStartTime,
                newEndTime)
        )
                .thenReturn(Optional.empty());


        mockMvc.perform(
                        put("/api/bookings/{id}/reschedule",
                                bookingId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturnBadRequestWhenRescheduleStartTimeIsNull()
            throws Exception {
        Long bookingId = 9999L;

        String requestBody = """
                {
                    "startTime": null,
                    "endTime": "2030-01-15T11:00:00"
                }
                """;

        mockMvc.perform(
                        put("/api/bookings/{id}/reschedule",
                                bookingId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(
                        jsonPath("$.code")
                                .value("VALIDATION_FAILED")
                )
                .andExpect(
                        jsonPath("$.message")
                                .value("Request validation failed")
                )
                .andExpect(
                        jsonPath(
                                "$.violations[?(@.field == 'startTime')].message"
                        ).value(
                                org.hamcrest.Matchers.hasItem(
                                        "Start time must not be null"
                                )
                        )
                );
        verifyNoInteractions(authorizationService);
    }

    @Test
    void shouldReturnBadRequestWhenRescheduleEndTimeIsNull()
            throws Exception {
        Long bookingId = 9999L;

        String requestBody = """
                {
                    "startTime": "2030-01-15T11:00:00",
                    "endTime": null
                }
                """;

        mockMvc.perform(
                        put("/api/bookings/{id}/reschedule",
                                bookingId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(
                        jsonPath("$.code")
                                .value("VALIDATION_FAILED")
                )
                .andExpect(
                        jsonPath("$.message")
                                .value("Request validation failed")
                )
                .andExpect(
                        jsonPath(
                                "$.violations[?(@.field == 'endTime')].message"
                        ).value(
                                org.hamcrest.Matchers.hasItem(
                                        "End time must not be null"
                                )
                        )
                );
        verifyNoInteractions(authorizationService);
    }

    @Test
    void shouldReturnConflictWhenRescheduleTimeConflicts()
            throws Exception {
        Long bookingId = 11L;
        LocalDateTime newStartTime =
                LocalDateTime.of(2030, 1, 15, 10, 0);

        LocalDateTime newEndTime =
                LocalDateTime.of(2030, 1, 15, 11, 0);

        String requestBody = """
                {
                  "startTime": "2030-01-15T10:00:00",
                  "endTime": "2030-01-15T11:00:00"
                }
                """;

        when(authorizationService.rescheduleBookingById(
                userContext,
                bookingId,
                newStartTime,
                newEndTime
        ))
                .thenThrow(
                        new BookingTimeConflictException(2L)
                );
        mockMvc.perform(
                        put("/api/bookings/{id}/reschedule",
                                bookingId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody))
                .andExpect(status().isConflict())
                .andExpect(
                        jsonPath("$.code")
                                .value("BOOKING_TIME_CONFLICT")
                )
                .andExpect(
                        jsonPath("$.message")
                                .value(
                                        "Bookable service is already booked "
                                                + "for the requested time, service id: 2"
                                )
                );
        verify(authorizationService)
                .rescheduleBookingById(
                        userContext,
                        bookingId,
                        newStartTime,
                        newEndTime
                );

    }
    @Test
    void shouldReturnConflictWhenBookingCannotBeRescheduled()
            throws Exception {
        Long bookingId = 11L;

        LocalDateTime newStartTime =
                LocalDateTime.of(2030, 1, 15, 10, 0);

        LocalDateTime newEndTime =
                LocalDateTime.of(2030, 1, 15, 11, 0);

        String requestBody = """
        {
          "startTime": "2030-01-15T10:00:00",
          "endTime": "2030-01-15T11:00:00"
        }
        """;
        when(authorizationService.rescheduleBookingById(
                userContext,
                bookingId,
                newStartTime,
                newEndTime
        ))
                .thenThrow(
                        new BookingCannotBeRescheduledException(
                                BookingStatus.CANCELLED
                        )
                );
        mockMvc.perform(
                        put(
                                "/api/bookings/{id}/reschedule",
                                bookingId
                        )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody)
                )
                .andExpect(status().isConflict())
        .andExpect(
                jsonPath("$.code")
                        .value("BOOKING_CANNOT_BE_RESCHEDULED")
        )
                .andExpect(
                        jsonPath("$.message")
                                .value(
                                        "Booking cannot be rescheduled from status: CANCELLED"
                                )
                );
        verify(authorizationService)
                .rescheduleBookingById(
                        userContext,
                        bookingId,
                        newStartTime,
                        newEndTime
                );

    }
}