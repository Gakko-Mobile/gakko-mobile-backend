package com.gakkomobile.messages.service;

import com.gakkomobile.user.Role;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;

@Component
public class WorkingHoursEvaluator {

    static final ZoneId WARSAW = ZoneId.of("Europe/Warsaw");
    private static final LocalTime OPENING = LocalTime.of(8, 0);
    private static final LocalTime CLOSING = LocalTime.of(20, 0);

    private final Clock clock;

    public WorkingHoursEvaluator(Clock clock) {

        this.clock = clock;
    }

    public boolean shouldBuffer(Role senderRole, boolean lecturerRecipient, boolean urgent) {

        return shouldBuffer(senderRole, lecturerRecipient, urgent, clock.instant());
    }

    public boolean shouldBuffer(Role senderRole, boolean lecturerRecipient, boolean urgent, Instant instant) {

        if (urgent || senderRole != Role.STUDENT || !lecturerRecipient) {
            return false;
        }
        return !isWithinWorkingHours(instant);
    }

    public boolean isWithinWorkingHours(Instant instant) {

        ZonedDateTime zoned = instant.atZone(WARSAW);
        LocalTime time = zoned.toLocalTime();
        return !time.isBefore(OPENING) && time.isBefore(CLOSING);
    }

    public OffsetDateTime nextBusinessOpening() {

        return nextBusinessOpening(clock.instant());
    }

    public OffsetDateTime nextBusinessOpening(Instant instant) {

        ZonedDateTime zoned = instant.atZone(WARSAW);
        LocalDate date = zoned.toLocalDate();
        if (isWeekday(date) && zoned.toLocalTime().isBefore(OPENING)) {
            return atOpening(date);
        }
        LocalDate next = date.plusDays(1);
        while (!isWeekday(next)) {
            next = next.plusDays(1);
        }
        return atOpening(next);
    }

    private static boolean isWeekday(LocalDate date) {

        DayOfWeek day = date.getDayOfWeek();
        return day != DayOfWeek.SATURDAY && day != DayOfWeek.SUNDAY;
    }

    private static OffsetDateTime atOpening(LocalDate date) {

        return date.atTime(OPENING).atZone(WARSAW).toOffsetDateTime();
    }
}
