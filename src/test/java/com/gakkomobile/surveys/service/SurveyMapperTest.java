package com.gakkomobile.surveys.service;

import com.gakkomobile.surveys.domain.AudienceTargetType;
import com.gakkomobile.surveys.domain.QuestionType;
import com.gakkomobile.surveys.domain.SurveyEntity;
import com.gakkomobile.surveys.dto.CreateSurveyCommand;
import com.gakkomobile.surveys.dto.SkipLogicDTO;
import com.gakkomobile.surveys.dto.SurveyQuestionDTO;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SurveyMapperTest {

    private final SurveyMapper mapper = new SurveyMapperImpl();

    @Test
    void toConfiguration_shouldApplyDefaultsAndCopyCollections() {

        List<String> targetIds = new ArrayList<>(List.of("CS-1"));
        CreateSurveyCommand command = new CreateSurveyCommand(
                "  Course feedback  ",
                null,
                List.of(new SurveyQuestionDTO(
                        " ",
                        QuestionType.SINGLE,
                        "Pace",
                        true,
                        null,
                        null,
                        null,
                        null,
                        null,
                        List.of(new SkipLogicDTO("A", "next"))
                )),
                null,
                targetIds,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );

        SurveyConfiguration configuration = mapper.toConfiguration(command);
        targetIds.add("CS-2");

        assertThat(configuration.title()).isEqualTo("  Course feedback  ");
        assertThat(configuration.targetType()).isEqualTo(AudienceTargetType.ALL);
        assertThat(configuration.anonymous()).isTrue();
        assertThat(configuration.requireLogin()).isTrue();
        assertThat(configuration.allowResubmission()).isFalse();
        assertThat(configuration.showResultsToStudents()).isFalse();
        assertThat(configuration.randomizeQuestionOrder()).isFalse();
        assertThat(configuration.targetIds()).containsExactly("CS-1");
        assertThat(configuration.reminderIntervals()).isEmpty();
        assertThat(configuration.questions()).hasSize(1);
        assertThat(configuration.questions().getFirst().id()).isNotBlank().isNotEqualTo(" ");
        assertThat(configuration.questions().getFirst().options()).isEmpty();
        assertThat(configuration.questions().getFirst().skipRules()).hasSize(1);
    }

    @Test
    void toConfiguration_shouldKeepExplicitFlags() {

        CreateSurveyCommand command = new CreateSurveyCommand(
                "Named",
                "Description",
                List.of(new SurveyQuestionDTO("q1", QuestionType.TEXT, "Open", false, null, null, null, null, null, null)),
                AudienceTargetType.GROUPS,
                List.of("CS-1"),
                false,
                true,
                false,
                true,
                true,
                10,
                "Thanks",
                null,
                null,
                List.of(1.0)
        );

        SurveyConfiguration configuration = mapper.toConfiguration(command);

        assertThat(configuration.anonymous()).isFalse();
        assertThat(configuration.allowResubmission()).isTrue();
        assertThat(configuration.requireLogin()).isFalse();
        assertThat(configuration.showResultsToStudents()).isTrue();
        assertThat(configuration.randomizeQuestionOrder()).isTrue();
        assertThat(configuration.questions().getFirst().id()).isEqualTo("q1");
    }

    @Test
    void applyConfiguration_shouldTrimTheTitleAndRoundTripDomainQuestions() {

        SurveyConfiguration configuration = mapper.toConfiguration(new CreateSurveyCommand(
                "  Trim me  ",
                "Description",
                List.of(new SurveyQuestionDTO("q1", QuestionType.TEXT, "Open", true, null, null, null, null, null, null)),
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
                null
        ));
        SurveyEntity survey = new SurveyEntity();

        mapper.applyConfiguration(survey, configuration);
        SurveyConfiguration reread = mapper.toConfiguration(survey);

        assertThat(survey.getTitle()).isEqualTo("Trim me");
        assertThat(reread.questions()).containsExactlyElementsOf(configuration.questions());
        assertThat(reread.targetIds()).isEmpty();
    }
}
