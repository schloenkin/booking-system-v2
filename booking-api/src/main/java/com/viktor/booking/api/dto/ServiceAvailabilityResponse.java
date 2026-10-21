package com.viktor.booking.api.dto;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public record ServiceAvailabilityResponse(
        Long serviceId,
        LocalDate date,
        List<SlotResponse> slots
) {
    public record SlotResponse(
            LocalTime time,
            boolean available
    ) {
    }
}