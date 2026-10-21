package com.viktor.booking.application.query;

import java.time.LocalTime;

public record AvailabilitySlot(
        LocalTime time,
        boolean available
) {
}
