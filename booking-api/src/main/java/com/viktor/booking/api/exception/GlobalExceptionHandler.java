package com.viktor.booking.api.exception;

import com.viktor.booking.api.dto.ApiErrorCode;
import com.viktor.booking.api.dto.ErrorResponse;
import com.viktor.booking.api.dto.ValidationViolation;
import com.viktor.booking.application.exception.BookableServiceNotFoundException;
import com.viktor.booking.application.exception.BookingInPastException;
import com.viktor.booking.application.exception.BookingOperationForbiddenException;
import com.viktor.booking.application.exception.BookingTimeConflictException;
import com.viktor.booking.application.exception.InactiveBookableServiceException;
import com.viktor.booking.application.exception.InvalidBookingDurationException;
import com.viktor.booking.application.exception.InvalidBookingSearchException;
import com.viktor.booking.application.exception.InvalidBookingTimeException;
import com.viktor.booking.application.exception.InvalidCredentialsException;
import com.viktor.booking.application.exception.UserAlreadyExistsException;
import com.viktor.booking.application.exception.UserNotFoundException;
import com.viktor.booking.domain.enums.BookingStatus;
import com.viktor.booking.domain.exception.BookingCannotBeCancelledException;
import com.viktor.booking.domain.exception.BookingCannotBeConfirmedException;
import com.viktor.booking.domain.exception.BookingCannotBeDeletedException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.converter.HttpMessageNotReadableException;

import java.util.Arrays;
import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ErrorResponse> handleResponseStatusException(
            ResponseStatusException exception,
            HttpServletRequest request
    ) {
        HttpStatus status = HttpStatus.resolve(
                exception.getStatusCode().value()
        );

        HttpStatus resolvedStatus =
                status != null
                        ? status
                        : HttpStatus.INTERNAL_SERVER_ERROR;

        ApiErrorCode code =
                resolvedStatus == HttpStatus.NOT_FOUND
                        ? ApiErrorCode.RESOURCE_NOT_FOUND
                        : ApiErrorCode.INTERNAL_ERROR;

        String message =
                exception.getReason() != null
                        ? exception.getReason()
                        : resolvedStatus.getReasonPhrase();

        return buildResponse(
                resolvedStatus,
                code,
                message,
                request
        );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(
            MethodArgumentNotValidException exception,
            HttpServletRequest request
    ) {
        List<ValidationViolation> violations =
                exception.getBindingResult()
                        .getFieldErrors()
                        .stream()
                        .map(fieldError ->
                                new ValidationViolation(
                                        fieldError.getField(),
                                        fieldError.getDefaultMessage()
                                )
                        )
                        .toList();

        ErrorResponse errorResponse =
                new ErrorResponse(
                        HttpStatus.BAD_REQUEST.value(),
                        HttpStatus.BAD_REQUEST.getReasonPhrase(),
                        ApiErrorCode.VALIDATION_FAILED,
                        "Request validation failed",
                        request.getRequestURI(),
                        violations
                );

        return ResponseEntity
                .badRequest()
                .body(errorResponse);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatchException(
            MethodArgumentTypeMismatchException exception,
            HttpServletRequest request
    ) {
        String message;

        if (exception.getRequiredType() == BookingStatus.class) {
            String allowedStatuses =
                    Arrays.stream(BookingStatus.values())
                            .map(Enum::name)
                            .toList()
                            .toString();

            message =
                    "Invalid booking status. Allowed values: "
                            + allowedStatuses;
        } else {
            message =
                    "Invalid value for parameter: "
                            + exception.getName();
        }

        return buildResponse(
                HttpStatus.BAD_REQUEST,
                ApiErrorCode.INVALID_PARAMETER,
                message,
                request
        );
    }

    @ExceptionHandler(InvalidBookingTimeException.class)
    public ResponseEntity<ErrorResponse> handleInvalidBookingTimeException(
            InvalidBookingTimeException exception,
            HttpServletRequest request
    ) {
        return buildResponse(
                HttpStatus.BAD_REQUEST,
                ApiErrorCode.INVALID_BOOKING_TIME,
                exception.getMessage(),
                request
        );
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleUserNotFoundException(
            UserNotFoundException exception,
            HttpServletRequest request
    ) {
        return buildResponse(
                HttpStatus.NOT_FOUND,
                ApiErrorCode.USER_NOT_FOUND,
                exception.getMessage(),
                request
        );
    }

    @ExceptionHandler(BookableServiceNotFoundException.class)
    public ResponseEntity<ErrorResponse>
    handleBookableServiceNotFoundException(
            BookableServiceNotFoundException exception,
            HttpServletRequest request
    ) {
        return buildResponse(
                HttpStatus.NOT_FOUND,
                ApiErrorCode.SERVICE_NOT_FOUND,
                exception.getMessage(),
                request
        );
    }

    @ExceptionHandler(BookingInPastException.class)
    public ResponseEntity<ErrorResponse> handleBookingInPastException(
            BookingInPastException exception,
            HttpServletRequest request
    ) {
        return buildResponse(
                HttpStatus.BAD_REQUEST,
                ApiErrorCode.BOOKING_IN_PAST,
                exception.getMessage(),
                request
        );
    }

    @ExceptionHandler(InvalidBookingDurationException.class)
    public ResponseEntity<ErrorResponse>
    handleInvalidBookingDurationException(
            InvalidBookingDurationException exception,
            HttpServletRequest request
    ) {
        return buildResponse(
                HttpStatus.BAD_REQUEST,
                ApiErrorCode.INVALID_BOOKING_DURATION,
                exception.getMessage(),
                request
        );
    }

    @ExceptionHandler(InvalidBookingSearchException.class)
    public ResponseEntity<ErrorResponse>
    handleInvalidBookingSearchException(
            InvalidBookingSearchException exception,
            HttpServletRequest request
    ) {
        return buildResponse(
                HttpStatus.BAD_REQUEST,
                ApiErrorCode.INVALID_BOOKING_SEARCH,
                exception.getMessage(),
                request
        );
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ErrorResponse>
    handleInvalidCredentialsException(
            InvalidCredentialsException exception,
            HttpServletRequest request
    ) {
        return buildResponse(
                HttpStatus.UNAUTHORIZED,
                ApiErrorCode.INVALID_CREDENTIALS,
                exception.getMessage(),
                request
        );
    }

    @ExceptionHandler(BookingOperationForbiddenException.class)
    public ResponseEntity<ErrorResponse> handleForbiddenException(
            BookingOperationForbiddenException exception,
            HttpServletRequest request
    ) {
        return buildResponse(
                HttpStatus.FORBIDDEN,
                ApiErrorCode.BOOKING_OPERATION_FORBIDDEN,
                exception.getMessage(),
                request
        );
    }

    @ExceptionHandler(InactiveBookableServiceException.class)
    public ResponseEntity<ErrorResponse>
    handleInactiveBookableServiceException(
            InactiveBookableServiceException exception,
            HttpServletRequest request
    ) {
        return buildResponse(
                HttpStatus.CONFLICT,
                ApiErrorCode.SERVICE_INACTIVE,
                exception.getMessage(),
                request
        );
    }

    @ExceptionHandler(BookingTimeConflictException.class)
    public ResponseEntity<ErrorResponse>
    handleBookingTimeConflictException(
            BookingTimeConflictException exception,
            HttpServletRequest request
    ) {
        return buildResponse(
                HttpStatus.CONFLICT,
                ApiErrorCode.BOOKING_TIME_CONFLICT,
                exception.getMessage(),
                request
        );
    }

    @ExceptionHandler(BookingCannotBeCancelledException.class)
    public ResponseEntity<ErrorResponse>
    handleBookingCannotBeCancelledException(
            BookingCannotBeCancelledException exception,
            HttpServletRequest request
    ) {
        return buildResponse(
                HttpStatus.CONFLICT,
                ApiErrorCode.BOOKING_CANNOT_BE_CANCELLED,
                exception.getMessage(),
                request
        );
    }

    @ExceptionHandler(BookingCannotBeConfirmedException.class)
    public ResponseEntity<ErrorResponse>
    handleBookingCannotBeConfirmedException(
            BookingCannotBeConfirmedException exception,
            HttpServletRequest request
    ) {
        return buildResponse(
                HttpStatus.CONFLICT,
                ApiErrorCode.BOOKING_CANNOT_BE_CONFIRMED,
                exception.getMessage(),
                request
        );
    }

    @ExceptionHandler(BookingCannotBeDeletedException.class)
    public ResponseEntity<ErrorResponse>
    handleBookingCannotBeDeletedException(
            BookingCannotBeDeletedException exception,
            HttpServletRequest request
    ) {
        return buildResponse(
                HttpStatus.CONFLICT,
                ApiErrorCode.BOOKING_CANNOT_BE_DELETED,
                exception.getMessage(),
                request
        );
    }

    @ExceptionHandler(UserAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse>
    handleUserAlreadyExistsException(
            UserAlreadyExistsException exception,
            HttpServletRequest request
    ) {
        return buildResponse(
                HttpStatus.CONFLICT,
                ApiErrorCode.USER_ALREADY_EXISTS,
                exception.getMessage(),
                request
        );
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleMalformedRequest(
            HttpMessageNotReadableException exception,
            HttpServletRequest request
    ) {
        return buildResponse(
                HttpStatus.BAD_REQUEST,
                ApiErrorCode.MALFORMED_REQUEST,
                "Request body contains malformed JSON",
                request
        );
    }

    private ResponseEntity<ErrorResponse> buildResponse(
            HttpStatus status,
            ApiErrorCode code,
            String message,
            HttpServletRequest request
    ) {
        ErrorResponse errorResponse =
                new ErrorResponse(
                        status.value(),
                        status.getReasonPhrase(),
                        code,
                        message,
                        request.getRequestURI()
                );

        return ResponseEntity
                .status(status)
                .body(errorResponse);
    }
}