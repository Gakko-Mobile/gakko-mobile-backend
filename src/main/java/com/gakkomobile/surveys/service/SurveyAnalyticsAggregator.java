package com.gakkomobile.surveys.service;

import com.gakkomobile.surveys.domain.QuestionType;
import com.gakkomobile.surveys.domain.SurveyEntity;
import com.gakkomobile.surveys.domain.SurveyQuestion;
import com.gakkomobile.surveys.domain.SurveyResponseEntity;
import com.gakkomobile.surveys.dto.QuestionStatisticsDTO;
import com.gakkomobile.surveys.dto.SurveyAnalyticsDTO;
import com.gakkomobile.surveys.dto.SurveyResultDTO;
import com.gakkomobile.surveys.dto.TextAnswerGroupDTO;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Component
class SurveyAnalyticsAggregator {

    private final SurveyResponseRepository surveyResponseRepository;
    private final DatabaseStudentAudienceLookup audienceLookup;

    SurveyAnalyticsAggregator(
            SurveyResponseRepository surveyResponseRepository,
            DatabaseStudentAudienceLookup audienceLookup
    ) {

        this.surveyResponseRepository = surveyResponseRepository;
        this.audienceLookup = audienceLookup;
    }

    SurveyAnalyticsDTO aggregateForStaff(SurveyEntity survey) {

        Aggregation aggregation = aggregate(survey);
        return SurveyAnalyticsDTO.builder()
                .surveyId(survey.getId())
                .responseCount(aggregation.responseCount())
                .audienceSize(aggregation.audienceSize())
                .responseLimit(survey.getResponseLimit())
                .completionRatio(aggregation.completionRatio())
                .questions(aggregation.questions())
                .build();
    }

    SurveyResultDTO aggregateForStudents(SurveyEntity survey) {

        Aggregation aggregation = aggregate(survey);
        return SurveyResultDTO.builder()
                .surveyId(survey.getId())
                .responseCount(aggregation.responseCount())
                .completionRatio(aggregation.completionRatio())
                .questions(aggregation.questions())
                .build();
    }

    private Aggregation aggregate(SurveyEntity survey) {

        List<SurveyResponseEntity> responses = surveyResponseRepository.findBySurveyId(survey.getId());
        Long audienceSize = audienceLookup.countAudience(survey.getTargetType(), survey.getTargetIds());
        double completionRatio = completionRatio(responses.size(), audienceSize, survey.getResponseLimit());
        List<QuestionStatisticsDTO> questions = new ArrayList<>();
        List<SurveyQuestion> surveyQuestions = Objects.isNull(survey.getQuestions()) ? List.of() : survey.getQuestions();
        for (SurveyQuestion question : surveyQuestions) {
            questions.add(questionStatistics(question, responses));
        }
        return new Aggregation(responses.size(), audienceSize, completionRatio, questions);
    }

    private QuestionStatisticsDTO questionStatistics(SurveyQuestion question, List<SurveyResponseEntity> responses) {

        if (question.type() == QuestionType.TEXT) {
            return QuestionStatisticsDTO.builder()
                    .questionId(question.id())
                    .type(question.type())
                    .title(question.title())
                    .distribution(Map.of())
                    .textResponses(groupTextAnswers(question.id(), responses))
                    .build();
        }
        return QuestionStatisticsDTO.builder()
                .questionId(question.id())
                .type(question.type())
                .title(question.title())
                .distribution(distribution(question, responses))
                .textResponses(List.of())
                .build();
    }

    private Map<String, Long> distribution(SurveyQuestion question, List<SurveyResponseEntity> responses) {

        Map<String, Long> distribution = new LinkedHashMap<>();
        if (question.type() == QuestionType.SCALE) {
            for (int scaleValue = question.scaleMin(); scaleValue <= question.scaleMax(); scaleValue++) {
                distribution.put(String.valueOf(scaleValue), 0L);
            }
        } else if (!Objects.isNull(question.options())) {
            for (String option : question.options()) {
                distribution.put(option, 0L);
            }
        }
        for (SurveyResponseEntity response : responses) {
            Object answerValue = answerValue(response, question.id());
            if (Objects.isNull(answerValue)) {
                continue;
            }
            if (question.type() == QuestionType.MULTIPLE && answerValue instanceof Collection<?> selectedValues) {
                for (Object selectedValue : selectedValues) {
                    increment(distribution, String.valueOf(selectedValue));
                }
                continue;
            }
            if (question.type() == QuestionType.SCALE && answerValue instanceof Number number) {
                increment(distribution, String.valueOf(number.intValue()));
                continue;
            }
            increment(distribution, String.valueOf(answerValue));
        }
        return distribution;
    }

    private List<TextAnswerGroupDTO> groupTextAnswers(String questionIdentifier, List<SurveyResponseEntity> responses) {

        Map<String, Long> groupedAnswers = new LinkedHashMap<>();
        for (SurveyResponseEntity response : responses) {
            Object answerValue = answerValue(response, questionIdentifier);
            if (!(answerValue instanceof String text) || text.isBlank()) {
                continue;
            }
            String normalizedText = text.trim();
            groupedAnswers.put(normalizedText, groupedAnswers.getOrDefault(normalizedText, 0L) + 1L);
        }
        List<TextAnswerGroupDTO> textResponses = new ArrayList<>();
        for (Map.Entry<String, Long> groupedAnswer : groupedAnswers.entrySet()) {
            textResponses.add(TextAnswerGroupDTO.builder()
                    .value(groupedAnswer.getKey())
                    .count(groupedAnswer.getValue())
                    .build());
        }
        textResponses.sort(Comparator.comparingLong(TextAnswerGroupDTO::count).reversed());
        return textResponses;
    }

    private Object answerValue(SurveyResponseEntity response, String questionIdentifier) {

        if (Objects.isNull(response.getAnswers())) {
            return null;
        }
        return response.getAnswers().get(questionIdentifier);
    }

    private void increment(Map<String, Long> distribution, String key) {

        distribution.put(key, distribution.getOrDefault(key, 0L) + 1L);
    }

    private double completionRatio(int responseCount, Long audienceSize, Integer responseLimit) {

        if (!Objects.isNull(audienceSize) && audienceSize > 0) {
            return (double) responseCount / audienceSize;
        }
        if (!Objects.isNull(responseLimit) && responseLimit > 0) {
            return (double) responseCount / responseLimit;
        }
        return 0.0;
    }

    private record Aggregation(
            long responseCount,
            Long audienceSize,
            double completionRatio,
            List<QuestionStatisticsDTO> questions
    ) {
    }
}
