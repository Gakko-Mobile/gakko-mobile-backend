package com.gakkomobile.surveys.service;

import com.gakkomobile.surveys.domain.QuestionType;
import com.gakkomobile.surveys.domain.SkipRule;
import com.gakkomobile.surveys.domain.SurveyQuestion;
import com.gakkomobile.surveys.exception.SurveyValidationException;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Component
class SurveyQuestionDefinitionValidator {

    void validate(List<SurveyQuestion> questions) {

        if (Objects.isNull(questions) || questions.isEmpty()) {
            throw new SurveyValidationException("Survey must contain at least one question");
        }
        Set<String> questionIdentifiers = new HashSet<>();
        for (SurveyQuestion question : questions) {
            if (Objects.isNull(question.id()) || question.id().isBlank()) {
                throw new SurveyValidationException("Question identifier is required");
            }
            if (!questionIdentifiers.add(question.id())) {
                throw new SurveyValidationException("Question identifiers must be unique");
            }
            if (Objects.isNull(question.title()) || question.title().isBlank()) {
                throw new SurveyValidationException("Question title is required");
            }
            if (Objects.isNull(question.type())) {
                throw new SurveyValidationException("Question type is required");
            }
            validateQuestionShape(question);
        }
        for (int questionIndex = 0; questionIndex < questions.size(); questionIndex++) {
            validateSkipRules(questions, questionIndex);
        }
    }

    private void validateQuestionShape(SurveyQuestion question) {

        if (question.type() == QuestionType.SINGLE || question.type() == QuestionType.MULTIPLE) {
            if (Objects.isNull(question.options()) || question.options().size() < 2) {
                throw new SurveyValidationException("Choice questions require at least two options");
            }
            Set<String> optionValues = new HashSet<>();
            for (String option : question.options()) {
                if (Objects.isNull(option) || option.isBlank()) {
                    throw new SurveyValidationException("Choice options cannot be blank");
                }
                if (!optionValues.add(option)) {
                    throw new SurveyValidationException("Choice options must be unique");
                }
            }
        }
        if (question.type() == QuestionType.SCALE) {
            if (Objects.isNull(question.scaleMin()) || Objects.isNull(question.scaleMax())
                    || question.scaleMin() >= question.scaleMax()) {
                throw new SurveyValidationException("Scale questions require a minimum lower than the maximum");
            }
        }
    }

    private void validateSkipRules(List<SurveyQuestion> questions, int questionIndex) {

        SurveyQuestion question = questions.get(questionIndex);
        if (Objects.isNull(question.skipRules())) {
            return;
        }
        for (SkipRule skipRule : question.skipRules()) {
            if (Objects.isNull(skipRule.triggerOptionId()) || skipRule.triggerOptionId().isBlank()) {
                throw new SurveyValidationException("Skip rule trigger is required");
            }
            if (Objects.isNull(skipRule.jumpToQuestionId()) || skipRule.jumpToQuestionId().isBlank()) {
                throw new SurveyValidationException("Skip rule target is required");
            }
            int targetIndex = indexOf(questions, skipRule.jumpToQuestionId());
            if (targetIndex < 0) {
                throw new SurveyValidationException("Skip rule target does not match a question");
            }
            if (targetIndex <= questionIndex) {
                throw new SurveyValidationException("Skip rules can only move forward");
            }
            if (!triggerMatchesQuestion(question, skipRule.triggerOptionId())) {
                throw new SurveyValidationException("Skip rule trigger does not match the question");
            }
        }
    }

    private boolean triggerMatchesQuestion(SurveyQuestion question, String triggerOptionId) {

        if (question.type() == QuestionType.TEXT) {
            return true;
        }
        if (question.type() == QuestionType.SCALE) {
            try {
                int triggerValue = Integer.parseInt(triggerOptionId.trim());
                return triggerValue >= question.scaleMin() && triggerValue <= question.scaleMax();
            } catch (NumberFormatException exception) {
                return false;
            }
        }
        return !Objects.isNull(question.options()) && question.options().contains(triggerOptionId);
    }

    private int indexOf(List<SurveyQuestion> questions, String questionIdentifier) {

        for (int questionIndex = 0; questionIndex < questions.size(); questionIndex++) {
            if (questions.get(questionIndex).id().equals(questionIdentifier)) {
                return questionIndex;
            }
        }
        return -1;
    }
}
