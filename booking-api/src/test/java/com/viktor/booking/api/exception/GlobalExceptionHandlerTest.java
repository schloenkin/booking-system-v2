package com.viktor.booking.api.exception;

import com.viktor.booking.domain.exception.BookingInPastException;
import com.viktor.booking.application.exception.BookingOperationForbiddenException;
import com.viktor.booking.application.exception.InvalidCredentialsException;
import com.viktor.booking.application.exception.UserAlreadyExistsException;
import com.viktor.booking.application.exception.UserNotFoundException;
import com.viktor.booking.domain.enums.BookingStatus;
import com.viktor.booking.domain.exception.BookingCannotBeCancelledException;
import com.viktor.booking.domain.exception.BookingCannotBeDeletedException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class GlobalExceptionHandlerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(
                        new TestController()
                )
                .setControllerAdvice(
                        new GlobalExceptionHandler()
                )
                .build();
    }

    @Test
    void shouldReturnUnified404Error()
            throws Exception {

        ResultActions result =
                mockMvc.perform(
                                get("/test/not-found")
                        )
                        .andExpect(
                                status().isNotFound()
                        );

        assertErrorContract(
                result,
                404,
                "Not Found",
                "USER_NOT_FOUND",
                "/test/not-found"
        );
    }

    @Test
    void shouldReturnUnified400Error()
            throws Exception {

        ResultActions result =
                mockMvc.perform(
                                get("/test/bad-request")
                        )
                        .andExpect(
                                status().isBadRequest()
                        );

        assertErrorContract(
                result,
                400,
                "Bad Request",
                "BOOKING_IN_PAST",
                "/test/bad-request"
        );
    }

    @Test
    void shouldReturnUnified409StateConflict()
            throws Exception {

        ResultActions result =
                mockMvc.perform(
                                get("/test/conflict")
                        )
                        .andExpect(
                                status().isConflict()
                        );

        assertErrorContract(
                result,
                409,
                "Conflict",
                "BOOKING_CANNOT_BE_CANCELLED",
                "/test/conflict"
        );
    }

    @Test
    void shouldReturnUnified409DeleteConflict()
            throws Exception {

        ResultActions result =
                mockMvc.perform(
                                get("/test/delete-conflict")
                        )
                        .andExpect(
                                status().isConflict()
                        );

        assertErrorContract(
                result,
                409,
                "Conflict",
                "BOOKING_CANNOT_BE_DELETED",
                "/test/delete-conflict"
        );
    }

    @Test
    void shouldReturnUnified401Error()
            throws Exception {

        ResultActions result =
                mockMvc.perform(
                                get("/test/invalid-credentials")
                        )
                        .andExpect(
                                status().isUnauthorized()
                        );

        assertErrorContract(
                result,
                401,
                "Unauthorized",
                "INVALID_CREDENTIALS",
                "/test/invalid-credentials"
        );
    }

    @Test
    void shouldReturnUnified403Error()
            throws Exception {

        ResultActions result =
                mockMvc.perform(
                                get("/test/forbidden")
                        )
                        .andExpect(
                                status().isForbidden()
                        );

        assertErrorContract(
                result,
                403,
                "Forbidden",
                "BOOKING_OPERATION_FORBIDDEN",
                "/test/forbidden"
        );
    }

    @Test
    void shouldReturnUnified409ForExistingUser()
            throws Exception {

        ResultActions result =
                mockMvc.perform(
                                get("/test/user-already-exists")
                        )
                        .andExpect(
                                status().isConflict()
                        );

        assertErrorContract(
                result,
                409,
                "Conflict",
                "USER_ALREADY_EXISTS",
                "/test/user-already-exists"
        );
    }

    @Test
    void shouldReturnAllValidationDetails()
            throws Exception {

        mockMvc.perform(
                        post("/test/validation")
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(
                                        """
                                        {
                                          "email": "invalid-email"
                                        }
                                        """
                                )
                )
                .andExpect(
                        status().isBadRequest()
                )
                .andExpect(
                        jsonPath("$.timestamp")
                                .isNotEmpty()
                )
                .andExpect(
                        jsonPath("$.status")
                                .value(400)
                )
                .andExpect(
                        jsonPath("$.error")
                                .value("Bad Request")
                )
                .andExpect(
                        jsonPath("$.code")
                                .value("VALIDATION_FAILED")
                )
                .andExpect(
                        jsonPath("$.message")
                                .value(
                                        "Request validation failed"
                                )
                )
                .andExpect(
                        jsonPath("$.path")
                                .value("/test/validation")
                )
                .andExpect(
                        jsonPath("$.violations.length()")
                                .value(1)
                )
                .andExpect(
                        jsonPath("$.violations[0].field")
                                .value("email")
                )
                .andExpect(
                        jsonPath("$.violations[0].message")
                                .value("Email must be valid")
                );
    }

    @Test
    void shouldReturnInvalidParameterForUnknownStatus()
            throws Exception {

        ResultActions result =
                mockMvc.perform(
                                get("/test/type-mismatch")
                                        .param(
                                                "status",
                                                "UNKNOWN"
                                        )
                        )
                        .andExpect(
                                status().isBadRequest()
                        );

        assertErrorContract(
                result,
                400,
                "Bad Request",
                "INVALID_PARAMETER",
                "/test/type-mismatch"
        );

        result.andExpect(
                jsonPath("$.message")
                        .value(
                                "Invalid booking status. "
                                        + "Allowed values: "
                                        + "[PENDING, CONFIRMED, "
                                        + "CANCELLED]"
                        )
        );
    }

    @Test
    void shouldReturnUnifiedErrorForMalformedJson()
            throws Exception {

        ResultActions result =
                mockMvc.perform(
                                post("/test/validation")
                                        .contentType(
                                                MediaType.APPLICATION_JSON
                                        )
                                        .content(
                                                """
                                                {
                                                  "email": "test@example.com"
                                                """
                                        )
                        )
                        .andExpect(
                                status().isBadRequest()
                        );

        assertErrorContract(
                result,
                400,
                "Bad Request",
                "MALFORMED_REQUEST",
                "/test/validation"
        );

        result.andExpect(
                jsonPath("$.message")
                        .value(
                                "Request body contains malformed JSON"
                        )
        );
    }

    private void assertErrorContract(
            ResultActions result,
            int expectedStatus,
            String expectedError,
            String expectedCode,
            String expectedPath
    ) throws Exception {

        result
                .andExpect(
                        jsonPath("$.timestamp")
                                .isNotEmpty()
                )
                .andExpect(
                        jsonPath("$.status")
                                .value(expectedStatus)
                )
                .andExpect(
                        jsonPath("$.error")
                                .value(expectedError)
                )
                .andExpect(
                        jsonPath("$.code")
                                .value(expectedCode)
                )
                .andExpect(
                        jsonPath("$.message")
                                .isNotEmpty()
                )
                .andExpect(
                        jsonPath("$.path")
                                .value(expectedPath)
                )
                .andExpect(
                        jsonPath("$.violations")
                                .isArray()
                )
                .andExpect(
                        jsonPath("$.violations.length()")
                                .value(0)
                );
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

        @PostMapping("/test/validation")
        void validation(
                @Valid
                @RequestBody
                ValidationRequest request
        ) {
        }

        @GetMapping("/test/type-mismatch")
        void typeMismatch(
                @RequestParam("status")
                BookingStatus status
        ) {
        }
    }

    static class ValidationRequest {

        @NotBlank(message = "Email must not be blank")
        @Email(message = "Email must be valid")
        private String email;

        public String getEmail() {
            return email;
        }

        public void setEmail(
                String email
        ) {
            this.email = email;
        }
    }
}