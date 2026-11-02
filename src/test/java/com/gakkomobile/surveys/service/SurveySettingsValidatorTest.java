package com.gakkomobile.surveys.service;

import com.gakkomobile.surveys.domain.AudienceTargetType;
import com.gakkomobile.surveys.domain.QuestionType;
import com.gakkomobile.surveys.domain.SurveyQuestion;
import com.gakkomobile.surveys.exception.SurveyValidationException;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SurveySettingsValidatorTest {

    private final SurveySettingsValidator validator = new SurveySettingsValidator();
    private final SurveyConfigurationValidator configurationValidator = new SurveyConfigurationValidator(
            validator,
            new SurveyQuestionDefinitionValidator()
    );

    @Test
    void validate_shouldAcceptACompleteConfiguration() {

        assertThatCode(() -> validator.validate(configuration("Feedback", true, false))).doesNotThrowAnyException();
    }

    @Test
    void validate_shouldRejectABlankTitle() {

        assertThatThrownBy(() -> validator.validate(configuration("  ", true, false)))
                .isInstanceOf(SurveyValidationException.class)
                .hasMessage("Survey title is required");
    }

    @Test
    void validate_shouldRejectAnonymousResubmission() {

        assertThatThrownBy(() -> validator.validate(configuration("Feedback", true, true)))
                .isInstanceOf(SurveyValidationException.class)
                .hasMessage("Anonymous surveys cannot allow resubmission");
    }

    @Test
    void validate_shouldRequireTargetsForGroupAndCourseAudiences() {

        SurveyConfiguration groups = configuration("Feedback", true, false, AudienceTargetType.GROUPS, List.of());
        SurveyConfiguration course = configuration("Feedback", true, false, AudienceTargetType.COURSE, null);

        assertThatThrownBy(() -> validator.validate(groups))
                .isInstanceOf(SurveyValidationException.class)
                .hasMessage("Target identifiers are required for the selected audience");
        assertThatThrownBy(() -> validator.validate(course))
                .isInstanceOf(SurveyValidationException.class)
                .hasMessage("Target identifiers are required for the selected audience");
    }

    @Test
    void validate_shouldRejectAnOpeningTimeThatIsNotBeforeClosing() {

        OffsetDateTime closingTime = OffsetDateTime.parse("2026-05-02T10:00:00Z");
        SurveyConfiguration configuration = configuration(
                "Feedback",
                true,
                false,
                AudienceTargetType.ALL,
                List.of(),
                closingTime,
                closingTime,
                List.of(),
                null
        );

        assertThatThrownBy(() -> validator.validate(configuration))
                .isInstanceOf(SurveyValidationException.class)
                .hasMessage("Opening time must be before closing time");
    }

    @Test
    void validate_shouldRequireAClosingTimeWhenRemindersAreConfigured() {

        SurveyConfiguration configuration = configuration(
                "Feedback",
                true,
                false,
                AudienceTargetType.ALL,
                List.of(),
                null,
                null,
                List.of(1.0),
                null
        );

        assertThatThrownBy(() -> validator.validate(configuration))
                .isInstanceOf(SurveyValidationException.class)
                .hasMessage("Closing time is required when reminders are configured");
    }

    @Test
    void validate_shouldRejectNonPositiveReminderIntervalsAndResponseLimits() {

        OffsetDateTime closingTime = OffsetDateTime.parse("2026-05-02T10:00:00Z");
        SurveyConfiguration reminders = configuration(
                "Feedback",
                true,
                false,
                AudienceTargetType.ALL,
                List.of(),
                null,
                closingTime,
                List.of(0.0),
                null
        );
        SurveyConfiguration responseLimit = configuration(
                "Feedback",
                true,
                false,
                AudienceTargetType.ALL,
                List.of(),
                null,
                null,
                List.of(),
                0
        );

        assertThatThrownBy(() -> validator.validate(reminders))
                .isInstanceOf(SurveyValidationException.class)
                .hasMessage("Reminder intervals must be greater than zero");
        assertThatThrownBy(() -> validator.validate(responseLimit))
                .isInstanceOf(SurveyValidationException.class)
                .hasMessage("Response limit must be greater than zero");
    }

    @Test
    void validate_shouldDelegateQuestionChecksAfterSettingsPass() {

        SurveyConfiguration configuration = new SurveyConfiguration(
                "Feedback",
                null,
                List.of(),
                AudienceTargetType.ALL,
                List.of(),
                true,
                false,
                true,
                false,
                false,
                null,
                null,
                null,
                null,
                List.of()
        );

        assertThatThrownBy(() -> configurationValidator.validate(configuration))
                .isInstanceOf(SurveyValidationException.class)
                .hasMessage("Survey must contain at least one question");
    }

    private SurveyConfiguration configuration(String title, boolean anonymous, boolean allowResubmission) {

        return configuration(title, anonymous, allowResubmission, AudienceTargetType.ALL, List.of(), null, null, List.of(), null);
    }

    private SurveyConfiguration configuration(
            String title,
            boolean anonymous,
            boolean allowResubmission,
            AudienceTargetType targetType,
            List<String> targetIds
    ) {

        return configuration(title, anonymous, allowResubmission, targetType, targetIds, null, null, List.of(), null);
    }

    private SurveyConfiguration configuration(
            String title,
            boolean anonymous,
            boolean allowResubmission,
            AudienceTargetType targetType,
            List<String> targetIds,
            OffsetDateTime opensAt,
            OffsetDateTime closesAt,
            List<Double> reminderIntervals,
            Integer responseLimit
    ) {

        return new SurveyConfiguration(
                title,
                "Description",
                List.of(new SurveyQuestion("q1", QuestionType.TEXT, "Question", true, List.of(), null, null, null, null, List.of())),
                targetType,
                targetIds,
                anonymous,
                allowResubmission,
                true,
                false,
                false,
                responseLimit,
                null,
                opensAt,
                closesAt,
                reminderIntervals
        );
    }
}
