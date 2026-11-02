package com.gakkomobile.surveys.service;

import com.gakkomobile.surveys.domain.AudienceTargetType;
import com.gakkomobile.surveys.domain.QuestionType;
import com.gakkomobile.surveys.domain.SurveyEntity;
import com.gakkomobile.surveys.domain.SurveyQuestion;
import com.gakkomobile.surveys.domain.SurveyResponseEntity;
import com.gakkomobile.surveys.dto.QuestionStatisticsDTO;
import com.gakkomobile.surveys.dto.SurveyAnalyticsDTO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SurveyAnalyticsAggregatorTest {

    @Mock
    private SurveyResponseRepository surveyResponseRepository;
    @Mock
    private DatabaseStudentAudienceLookup audienceLookup;

    @InjectMocks
    private SurveyAnalyticsAggregator aggregator;

    @Test
    void aggregateForStaff_shouldCountChoiceScaleAndTextAnswersAsIntegers() {

        UUID surveyIdentifier = UUID.randomUUID();
        SurveyEntity survey = new SurveyEntity();
        survey.setId(surveyIdentifier);
        survey.setTargetType(AudienceTargetType.ALL);
        survey.setResponseLimit(10);
        survey.setQuestions(List.of(
                new SurveyQuestion("choice", QuestionType.SINGLE, "Choice", true, List.of("A", "B"), null, null, null, null, List.of()),
                new SurveyQuestion("scale", QuestionType.SCALE, "Scale", true, List.of(), 1, 2, null, null, List.of()),
                new SurveyQuestion("text", QuestionType.TEXT, "Text", false, List.of(), null, null, null, null, List.of())
        ));
        when(surveyResponseRepository.findBySurveyId(surveyIdentifier)).thenReturn(List.of(
                response(surveyIdentifier, Map.of("choice", "A", "scale", 1, "text", " Useful ")),
                response(surveyIdentifier, Map.of("choice", "A", "scale", 2, "text", "Useful")),
                response(surveyIdentifier, Map.of("choice", "B", "scale", 2.0, "text", "   "))
        ));
        when(audienceLookup.countAudience(AudienceTargetType.ALL, survey.getTargetIds())).thenReturn(4L);

        SurveyAnalyticsDTO analytics = aggregator.aggregateForStaff(survey);

        assertThat(analytics.responseCount()).isEqualTo(3);
        assertThat(analytics.audienceSize()).isEqualTo(4L);
        assertThat(analytics.completionRatio()).isEqualTo(0.75);
        QuestionStatisticsDTO choice = analytics.questions().get(0);
        QuestionStatisticsDTO scale = analytics.questions().get(1);
        QuestionStatisticsDTO text = analytics.questions().get(2);
        assertThat(choice.distribution()).containsExactly(Map.entry("A", 2), Map.entry("B", 1));
        assertThat(choice.distribution().get("A")).isInstanceOf(Integer.class);
        assertThat(scale.distribution()).containsExactly(Map.entry("1", 1), Map.entry("2", 2));
        assertThat(text.distribution()).isEmpty();
        assertThat(text.textResponses()).hasSize(1);
        assertThat(text.textResponses().getFirst().value()).isEqualTo("Useful");
        assertThat(text.textResponses().getFirst().count()).isEqualTo(2);
    }

    @Test
    void aggregateForStudents_shouldUseTheResponseLimitWhenTheAudienceSizeIsUnknown() {

        UUID surveyIdentifier = UUID.randomUUID();
        SurveyEntity survey = new SurveyEntity();
        survey.setId(surveyIdentifier);
        survey.setTargetType(AudienceTargetType.VIA_LINK);
        survey.setResponseLimit(4);
        survey.setQuestions(List.of());
        when(surveyResponseRepository.findBySurveyId(surveyIdentifier)).thenReturn(List.of(response(surveyIdentifier, Map.of())));
        when(audienceLookup.countAudience(AudienceTargetType.VIA_LINK, survey.getTargetIds())).thenReturn(null);

        var result = aggregator.aggregateForStudents(survey);

        assertThat(result.responseCount()).isEqualTo(1);
        assertThat(result.completionRatio()).isEqualTo(0.25);
    }

    private SurveyResponseEntity response(UUID surveyIdentifier, Map<String, Object> answers) {

        SurveyResponseEntity response = new SurveyResponseEntity();
        response.setSurveyId(surveyIdentifier);
        response.setAnswers(new LinkedHashMap<>(answers));
        return response;
    }
}
