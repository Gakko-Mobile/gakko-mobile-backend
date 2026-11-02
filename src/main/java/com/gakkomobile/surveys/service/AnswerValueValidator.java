package com.gakkomobile.surveys.service;

import com.gakkomobile.surveys.domain.QuestionType;
import com.gakkomobile.surveys.domain.SurveyQuestion;
import com.gakkomobile.surveys.exception.SurveyValidationException;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;

@Component
class AnswerValueValidator {

    void validate(SurveyQuestion question, Object answerValue) {

        if (question.type() == QuestionType.SINGLE) {
            if (!(answerValue instanceof String selectedOption) || Objects.isNull(question.options())
                    || !question.options().contains(selectedOption)) {
                throw new SurveyValidationException("Single choice answer must be one of the options");
            }
            return;
        }
        if (question.type() == QuestionType.MULTIPLE) {
            List<String> selectedOptions = AnswerValues.selectedOptions(answerValue);
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
        int selectedScale = AnswerValues.scaleValue(answerValue);
        if (selectedScale < question.scaleMin() || selectedScale > question.scaleMax()) {
            throw new SurveyValidationException("Scale answer is outside the configured range");
        }
    }
}
