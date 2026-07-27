package com.viktor.booking.api.exception;

import com.viktor.booking.application.exception.BookingAlreadyCancelledException;
import com.viktor.booking.application.exception.BookingInPastException;
import com.viktor.booking.application.exception.InvalidCredentialsException;
import com.viktor.booking.application.exception.UserAlreadyExistsException;
import com.viktor.booking.application.exception.UserNotFoundException;
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
    void shouldReturn401WhenCredentialsAreInvalid()
            throws Exception {

        mockMvc.perform(
                        get("/test/invalid-credentials")
                )
                .andExpect(status().isUnauthorized());
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
            throw new BookingAlreadyCancelledException(1L);
        }

        @GetMapping("/test/invalid-credentials")
        void invalidCredentials() {
            throw new InvalidCredentialsException();
        }

        @GetMapping("/test/user-already-exists")
        void userAlreadyExists() {
            throw new UserAlreadyExistsException(
                    "existing@example.com"
            );
        }
    }
}
