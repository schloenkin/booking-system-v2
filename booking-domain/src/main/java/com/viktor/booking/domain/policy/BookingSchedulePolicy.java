
package com.viktor.booking.domain.policy;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public class BookingSchedulePolicy {

    private static final LocalTime OPENING = LocalTime.of(9, 0);
    private static final LocalTime LUNCH_START = LocalTime.of(13, 0);
    private static final LocalTime LUNCH_END = LocalTime.of(14, 0);
    private static final LocalTime CLOSING = LocalTime.of(18, 0);

    public boolean isValid(
            LocalDateTime start,
            LocalDateTime end,
            LocalDate today
    ) {
        if (start == null || end == null || today == null) {
            return false;
        }

        if (!start.isBefore(end)) {
            return false;
        }

        LocalDate bookingDate = start.toLocalDate();

        if (!bookingDate.equals(end.toLocalDate())) {
            return false;
        }

        if (!isWorkingDay(bookingDate)) {
            return false;
        }

        if (bookingDate.isBefore(nextWorkingDay(today))) {
            return false;
        }

        LocalTime startTime = start.toLocalTime();
        LocalTime endTime = end.toLocalTime();

        boolean beforeLunch =
                !startTime.isBefore(OPENING)
                        && !endTime.isAfter(LUNCH_START);

        boolean afterLunch =
                !startTime.isBefore(LUNCH_END)
                        && !endTime.isAfter(CLOSING);

        return beforeLunch || afterLunch;
    }

    public LocalDate nextWorkingDay(LocalDate today) {
        LocalDate date = today.plusDays(1);

        while (!isWorkingDay(date)) {
            date = date.plusDays(1);
        }

        return date;
    }

    public boolean isWorkingDay(LocalDate date) {
        DayOfWeek day = date.getDayOfWeek();

        return day != DayOfWeek.SATURDAY
                && day != DayOfWeek.SUNDAY;
    }
}
