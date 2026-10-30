package com.gakkomobile.surveys.service;

import com.gakkomobile.surveys.domain.QuestionType;
import com.gakkomobile.surveys.domain.SkipRule;
import com.gakkomobile.surveys.domain.SurveyQuestion;
import com.gakkomobile.surveys.dto.AnswerDTO;
import com.gakkomobile.surveys.exception.SurveyValidationException;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Component
class SurveyAnswerValidator {

    Map<String, Object> toAnswerMap(List<AnswerDTO> answers) {

        Map<String, Object> answerMap = new LinkedHashMap<>();
        if (Objects.isNull(answers)) {
            return answerMap;
        }
        for (AnswerDTO answer : answers) {
            if (answerMap.containsKey(answer.questionId())) {
                throw new SurveyValidationException("Each question can be answered only once");
            }
            answerMap.put(answer.questionId(), answer.answerValue());
        }
        return answerMap;
    }

    void validate(List<SurveyQuestion> questions, Map<String, Object> answers) {

        Set<String> knownQuestionIdentifiers = new HashSet<>();
        for (SurveyQuestion question : questions) {
            knownQuestionIdentifiers.add(question.id());
        }
        for (String questionIdentifier : answers.keySet()) {
            if (!knownQuestionIdentifiers.contains(questionIdentifier)) {
                throw new SurveyValidationException("Answer does not match a survey question");
            }
        }

        Set<String> hiddenQuestionIdentifiers = hiddenQuestionIdentifiers(questions, answers);
        for (SurveyQuestion question : questions) {
            if (hiddenQuestionIdentifiers.contains(question.id())) {
                continue;
            }
            Object answerValue = answers.get(question.id());
            if (question.isRequired() && isEmpty(answerValue)) {
                throw new SurveyValidationException("A required question is missing an answer");
            }
            if (isEmpty(answerValue)) {
                continue;
            }
            validateShape(question, answerValue);
        }
    }

    private Set<String> hiddenQuestionIdentifiers(List<SurveyQuestion> questions, Map<String, Object> answers) {

        Set<String> hiddenQuestionIdentifiers = new HashSet<>();
        for (int questionIndex = 0; questionIndex < questions.size(); questionIndex++) {
            SurveyQuestion question = questions.get(questionIndex);
            if (hiddenQuestionIdentifiers.contains(question.id()) || Objects.isNull(question.skipRules())) {
                continue;
            }
            Object answerValue = answers.get(question.id());
            for (SkipRule skipRule : question.skipRules()) {
                if (!matchesTrigger(question, answerValue, skipRule.triggerOptionId())) {
                    continue;
                }
                int targetIndex = indexOf(questions, skipRule.jumpToQuestionId());
                if (targetIndex <= questionIndex) {
                    break;
                }
                for (int hiddenIndex = questionIndex + 1; hiddenIndex < targetIndex; hiddenIndex++) {
                    hiddenQuestionIdentifiers.add(questions.get(hiddenIndex).id());
                }
                break;
            }
        }
        return hiddenQuestionIdentifiers;
    }

    private boolean matchesTrigger(SurveyQuestion question, Object answerValue, String triggerOptionId) {

        if (isEmpty(answerValue) || Objects.isNull(triggerOptionId)) {
            return false;
        }
        if (question.type() == QuestionType.MULTIPLE) {
            return selectedOptions(answerValue).contains(triggerOptionId);
        }
        if (question.type() == QuestionType.SCALE) {
            return String.valueOf(scaleValue(answerValue)).equals(triggerOptionId.trim());
        }
        return String.valueOf(answerValue).equals(triggerOptionId);
    }

    private void validateShape(SurveyQuestion question, Object answerValue) {

        if (question.type() == QuestionType.SINGLE) {
            if (!(answerValue instanceof String selectedOption) || Objects.isNull(question.options())
                    || !question.options().contains(selectedOption)) {
                throw new SurveyValidationException("Single choice answer must be one of the options");
            }
            return;
        }
        if (question.type() == QuestionType.MULTIPLE) {
            List<String> selectedOptions = selectedOptions(answerValue);
            if (selectedOptions.isEmpty()) {
                throw new SurveyValidationException("Multiple choice answer must include at least one option");
            }
            for (String selectedOption : selectedOptions) {
                if (Objects.isNull(question.options()) || !question.options().contains(selectedOption)) {
                    throw new SurveyValidationException("Multiple choice answer must use the provided options");
                }
            }
            return;
        }
        if (question.type() == QuestionType.TEXT) {
            if (!(answerValue instanceof String)) {
                throw new SurveyValidationException("Text answer must be text");
            }
            return;
        }
        int selectedScale = scaleValue(answerValue);
        if (selectedScale < question.scaleMin() || selectedScale > question.scaleMax()) {
            throw new SurveyValidationException("Scale answer is outside the configured range");
        }
    }

    private List<String> selectedOptions(Object answerValue) {

        if (!(answerValue instanceof Collection<?> selectedValues)) {
            throw new SurveyValidationException("Multiple choice answer must be a list of options");
        }
        List<String> selectedOptions = new ArrayList<>();
        for (Object selectedValue : selectedValues) {
            if (!(selectedValue instanceof String selectedOption)) {
                throw new SurveyValidationException("Multiple choice answer must be a list of options");
            }
            selectedOptions.add(selectedOption);
        }
        return selectedOptions;
    }

    private int scaleValue(Object answerValue) {

        if (answerValue instanceof Number number) {
            double numericValue = number.doubleValue();
            if (numericValue != Math.rint(numericValue)) {
                throw new SurveyValidationException("Scale answer must be a whole number");
            }
            return number.intValue();
        }
        if (answerValue instanceof String text) {
            try {
                return Integer.parseInt(text.trim());
            } catch (NumberFormatException exception) {
                throw new SurveyValidationException("Scale answer must be a whole number");
            }
        }
        throw new SurveyValidationException("Scale answer must be a whole number");
    }

    private boolean isEmpty(Object answerValue) {

        if (Objects.isNull(answerValue)) {
            return true;
        }
        if (answerValue instanceof String text) {
            return text.isBlank();
        }
        if (answerValue instanceof Collection<?> values) {
            return values.isEmpty();
        }
        return false;
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
