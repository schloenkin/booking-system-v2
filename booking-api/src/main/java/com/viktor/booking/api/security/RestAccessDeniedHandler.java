
package com.viktor.booking.api.security;

import com.viktor.booking.api.dto.ApiErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class RestAccessDeniedHandler
        implements AccessDeniedHandler {

    private final SecurityErrorResponseWriter errorResponseWriter;

    public RestAccessDeniedHandler(
            SecurityErrorResponseWriter errorResponseWriter
    ) {
        this.errorResponseWriter = errorResponseWriter;
    }

    @Override
    public void handle(
            HttpServletRequest request,
            HttpServletResponse response,
            AccessDeniedException exception
    ) throws IOException {

        errorResponseWriter.write(
                request,
                response,
                HttpStatus.FORBIDDEN,
                ApiErrorCode.ACCESS_DENIED,
                "Access is denied"
        );
    }
}
