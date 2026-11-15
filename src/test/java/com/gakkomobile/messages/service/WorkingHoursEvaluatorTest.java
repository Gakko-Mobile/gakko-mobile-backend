package com.gakkomobile.messages.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.gakkomobile.user.Role;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

class WorkingHoursEvaluatorTest {

    private static final ZoneId WARSAW = WorkingHoursEvaluator.WARSAW;

    private final WorkingHoursEvaluator evaluator = new WorkingHoursEvaluator(Clock.system(WARSAW));

    @Test
    void weekdayMorningIsInsideWorkingHours() {

        Instant instant = at(2026, 11, 11, 10, 0);

        assertThat(evaluator.isWithinWorkingHours(instant)).isTrue();
        assertThat(evaluator.shouldBuffer(Role.STUDENT, true, false, instant)).isFalse();
    }

    @Test
    void openingIsInclusiveAndClosingIsExclusive() {

        assertThat(evaluator.isWithinWorkingHours(at(2026, 11, 11, 8, 0))).isTrue();
        assertThat(evaluator.isWithinWorkingHours(at(2026, 11, 11, 16, 59))).isTrue();
        assertThat(evaluator.isWithinWorkingHours(at(2026, 11, 13, 17, 0))).isFalse();
    }

    @Test
    void earlyWeekdayOpensTheSameMorning() {

        Instant early = at(2026, 11, 16, 7, 30);

        assertThat(evaluator.nextBusinessOpening(early).toInstant()).isEqualTo(at(2026, 11, 16, 8, 0));
    }

    @Test
    void wednesdayEveningOpensThursdayMorning() {

        Instant evening = at(2026, 11, 11, 20, 0);

        assertThat(evaluator.shouldBuffer(Role.STUDENT, true, false, evening)).isTrue();
        assertThat(evaluator.nextBusinessOpening(evening).toInstant()).isEqualTo(at(2026, 11, 12, 8, 0));
    }

    @Test
    void fridayEveningAndWeekendOpenOnMonday() {

        assertThat(evaluator.nextBusinessOpening(at(2026, 11, 13, 17, 0)).toInstant())
                .isEqualTo(at(2026, 11, 16, 8, 0));
        assertThat(evaluator.nextBusinessOpening(at(2026, 11, 14, 12, 0)).toInstant())
                .isEqualTo(at(2026, 11, 16, 8, 0));
        assertThat(evaluator.nextBusinessOpening(at(2026, 11, 15, 12, 0)).toInstant())
                .isEqualTo(at(2026, 11, 16, 8, 0));
    }

    @Test
    void urgentStaffAndStudentToStudentMessagesAreNotBuffered() {

        Instant evening = at(2026, 11, 11, 20, 0);

        assertThat(evaluator.shouldBuffer(Role.STUDENT, true, true, evening)).isFalse();
        assertThat(evaluator.shouldBuffer(Role.STUDENT, false, false, evening)).isFalse();
        assertThat(evaluator.shouldBuffer(Role.LECTURER, true, false, evening)).isFalse();
    }

    @Test
    void clockMethodsUseTheInjectedClock() {

        Clock clock = Clock.fixed(at(2026, 11, 11, 20, 0), WARSAW);
        WorkingHoursEvaluator clocked = new WorkingHoursEvaluator(clock);

        assertThat(clocked.shouldBuffer(Role.STUDENT, true, false)).isTrue();
        assertThat(clocked.nextBusinessOpening().toInstant()).isEqualTo(at(2026, 11, 12, 8, 0));
    }

    private static Instant at(int year, int month, int day, int hour, int minute) {

        return LocalDateTime.of(year, month, day, hour, minute).atZone(WARSAW).toInstant();
    }
}
