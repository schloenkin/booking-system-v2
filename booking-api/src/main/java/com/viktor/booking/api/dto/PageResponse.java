package com.viktor.booking.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import java.util.List;

@Schema(
        name = "PageResponse",
        description = "Paginated collection of API resources"
)
public record PageResponse<T>(

        @ArraySchema(
                arraySchema = @Schema(
                        description = "Bookings contained in the current page",
                        requiredMode = Schema.RequiredMode.REQUIRED
                ),
                schema = @Schema(
                        implementation = BookingResponse.class
                )
        )
        List<T> content,

        @Schema(
                description = "Zero-based number of the current page",
                example = "0",
                minimum = "0",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        int page,

        @Schema(
                description = "Maximum number of resources requested per page",
                example = "20",
                minimum = "1",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        int size,

        @Schema(
                description = "Total number of resources matching the search criteria",
                example = "57",
                minimum = "0",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        long totalElements,

        @Schema(
                description = "Total number of available pages",
                example = "3",
                minimum = "0",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        int totalPages,

        @Schema(
                description = "Whether this is the first page",
                example = "true",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        boolean first,

        @Schema(
                description = "Whether this is the last page",
                example = "false",
                requiredMode = Schema.RequiredMode.REQUIRED
        )
        boolean last
) {
}