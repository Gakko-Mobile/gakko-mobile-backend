package com.gakkomobile.surveys.service;

import com.gakkomobile.surveys.domain.AudienceTargetType;
import com.gakkomobile.surveys.domain.SkipRule;
import com.gakkomobile.surveys.domain.SurveyQuestion;
import com.gakkomobile.surveys.dto.CreateSurveyCommand;
import com.gakkomobile.surveys.dto.SkipLogicDTO;
import com.gakkomobile.surveys.dto.SurveyQuestionDTO;
import com.gakkomobile.surveys.dto.UpdateSurveyCommand;
import com.github.f4b6a3.uuid.UuidCreator;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Component
class SurveyMapper {

    SurveyConfiguration toConfiguration(CreateSurveyCommand command) {

        return toConfiguration(
                command.title(),
                command.description(),
                command.questions(),
                command.targetType(),
                command.targetIds(),
                command.anonymous(),
                command.allowResubmission(),
                command.requireLogin(),
                command.showResultsToStudents(),
                command.randomizeQuestionOrder(),
                command.responseLimit(),
                command.confirmationMessage(),
                command.opensAt(),
                command.closesAt(),
                command.reminderIntervals()
        );
    }

    SurveyConfiguration toConfiguration(UpdateSurveyCommand command) {

        return toConfiguration(
                command.title(),
                command.description(),
                command.questions(),
                command.targetType(),
                command.targetIds(),
                command.anonymous(),
                command.allowResubmission(),
                command.requireLogin(),
                command.showResultsToStudents(),
                command.randomizeQuestionOrder(),
                command.responseLimit(),
                command.confirmationMessage(),
                command.opensAt(),
                command.closesAt(),
                command.reminderIntervals()
        );
    }

    private SurveyConfiguration toConfiguration(
            String title,
            String description,
            List<SurveyQuestionDTO> questions,
            AudienceTargetType targetType,
            List<String> targetIds,
            Boolean anonymous,
            Boolean allowResubmission,
            Boolean requireLogin,
            Boolean showResultsToStudents,
            Boolean randomizeQuestionOrder,
            Integer responseLimit,
            String confirmationMessage,
            OffsetDateTime opensAt,
            OffsetDateTime closesAt,
            List<Double> reminderIntervals
    ) {

        AudienceTargetType resolvedTargetType = Objects.isNull(targetType) ? AudienceTargetType.ALL : targetType;
        boolean resolvedAnonymous = Objects.isNull(anonymous) || anonymous;
        boolean resolvedAllowResubmission = !Objects.isNull(allowResubmission) && allowResubmission;
        boolean resolvedRequireLogin = Objects.isNull(requireLogin) || requireLogin;
        boolean resolvedShowResults = !Objects.isNull(showResultsToStudents) && showResultsToStudents;
        boolean resolvedRandomize = !Objects.isNull(randomizeQuestionOrder) && randomizeQuestionOrder;

        return new SurveyConfiguration(
                title,
                description,
                toDomainQuestions(questions),
                resolvedTargetType,
                copyStrings(targetIds),
                resolvedAnonymous,
                resolvedAllowResubmission,
                resolvedRequireLogin,
                resolvedShowResults,
                resolvedRandomize,
                responseLimit,
                confirmationMessage,
                opensAt,
                closesAt,
                copyDoubles(reminderIntervals)
        );
    }

    List<SurveyQuestion> toDomainQuestions(List<SurveyQuestionDTO> questions) {

        List<SurveyQuestion> domainQuestions = new ArrayList<>();
        if (Objects.isNull(questions)) {
            return domainQuestions;
        }
        for (SurveyQuestionDTO question : questions) {
            domainQuestions.add(toDomainQuestion(question));
        }
        return domainQuestions;
    }

    SurveyQuestionDTO toApiQuestion(SurveyQuestion question) {

        return new SurveyQuestionDTO(
                question.id(),
                question.type(),
                question.title(),
                question.isRequired(),
                copyStrings(question.options()),
                question.scaleMin(),
                question.scaleMax(),
                question.labelMin(),
                question.labelMax(),
                toApiSkipRules(question.skipRules())
        );
    }

    List<SurveyQuestionDTO> toApiQuestions(List<SurveyQuestion> questions) {

        List<SurveyQuestionDTO> mappedQuestions = new ArrayList<>();
        if (Objects.isNull(questions)) {
            return mappedQuestions;
        }
        for (SurveyQuestion question : questions) {
            mappedQuestions.add(toApiQuestion(question));
        }
        return mappedQuestions;
    }

    private SurveyQuestion toDomainQuestion(SurveyQuestionDTO question) {

        String questionIdentifier = question.id();
        if (Objects.isNull(questionIdentifier) || questionIdentifier.isBlank()) {
            questionIdentifier = UuidCreator.getTimeOrderedEpoch().toString();
        }
        return new SurveyQuestion(
                questionIdentifier,
                question.type(),
                question.title(),
                question.isRequired(),
                copyStrings(question.options()),
                question.scaleMin(),
                question.scaleMax(),
                question.labelMin(),
                question.labelMax(),
                toSkipRules(question.skipRules())
        );
    }

    private List<SkipRule> toSkipRules(List<SkipLogicDTO> skipRules) {

        List<SkipRule> domainRules = new ArrayList<>();
        if (Objects.isNull(skipRules)) {
            return domainRules;
        }
        for (SkipLogicDTO skipRule : skipRules) {
            domainRules.add(new SkipRule(skipRule.triggerOptionId(), skipRule.jumpToQuestionId()));
        }
        return domainRules;
    }

    private List<SkipLogicDTO> toApiSkipRules(List<SkipRule> skipRules) {

        List<SkipLogicDTO> mappedSkipRules = new ArrayList<>();
        if (Objects.isNull(skipRules)) {
            return mappedSkipRules;
        }
        for (SkipRule skipRule : skipRules) {
            mappedSkipRules.add(new SkipLogicDTO(skipRule.triggerOptionId(), skipRule.jumpToQuestionId()));
        }
        return mappedSkipRules;
    }

    private List<String> copyStrings(List<String> values) {

        if (Objects.isNull(values)) {
            return new ArrayList<>();
        }
        return new ArrayList<>(values);
    }

    private List<Double> copyDoubles(List<Double> values) {

        if (Objects.isNull(values)) {
            return new ArrayList<>();
        }
        return new ArrayList<>(values);
    }
}
