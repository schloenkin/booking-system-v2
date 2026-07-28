package com.viktor.booking.api.exception;

import com.viktor.booking.domain.enums.BookingStatus;
import com.viktor.booking.domain.exception.BookingCannotBeCancelledException;
import com.viktor.booking.domain.exception.BookingCannotBeDeletedException;
import com.viktor.booking.application.exception.BookingInPastException;
import com.viktor.booking.application.exception.InvalidCredentialsException;
import com.viktor.booking.application.exception.UserAlreadyExistsException;
import com.viktor.booking.application.exception.UserNotFoundException;
import com.viktor.booking.application.exception.BookingOperationForbiddenException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class GlobalExceptionHandlerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new TestController())
                .setControllerAdvice(
                        new GlobalExceptionHandler()
                )
                .build();
    }

    @Test
    void shouldReturn404WhenResourceIsNotFound()
            throws Exception {

        mockMvc.perform(get("/test/not-found"))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturn400WhenBookingDataIsInvalid()
            throws Exception {

        mockMvc.perform(get("/test/bad-request"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void shouldReturn409WhenBookingStateConflicts()
            throws Exception {

        mockMvc.perform(get("/test/conflict"))
                .andExpect(status().isConflict());
    }

    @Test
    void shouldReturn409WhenBookingCannotBeDeleted()
            throws Exception {

        mockMvc.perform(
                        get("/test/delete-conflict")
                )
                .andExpect(status().isConflict());
    }

    @Test
    void shouldReturn401WhenCredentialsAreInvalid()
            throws Exception {

        mockMvc.perform(
                        get("/test/invalid-credentials")
                )
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldReturn403WhenBookingOperationIsForbidden()
            throws Exception {

        mockMvc.perform(
                        get("/test/forbidden")
                )
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldReturn409WhenUserAlreadyExists()
            throws Exception {

        mockMvc.perform(
                        get("/test/user-already-exists")
                )
                .andExpect(status().isConflict());
    }

    @RestController
    static class TestController {

        @GetMapping("/test/not-found")
        void notFound() {
            throw new UserNotFoundException(1L);
        }

        @GetMapping("/test/bad-request")
        void badRequest() {
            throw new BookingInPastException();
        }

        @GetMapping("/test/conflict")
        void conflict() {
            throw new BookingCannotBeCancelledException(
                    BookingStatus.CANCELLED
            );
        }

        @GetMapping("/test/delete-conflict")
        void deleteConflict() {
            throw new BookingCannotBeDeletedException(
                    BookingStatus.CONFIRMED
            );
        }

        @GetMapping("/test/invalid-credentials")
        void invalidCredentials() {
            throw new InvalidCredentialsException();
        }

        @GetMapping("/test/forbidden")
        void forbidden() {
            throw new BookingOperationForbiddenException(
                    "delete"
            );
        }

        @GetMapping("/test/user-already-exists")
        void userAlreadyExists() {
            throw new UserAlreadyExistsException(
                    "existing@example.com"
            );
        }
    }
}
