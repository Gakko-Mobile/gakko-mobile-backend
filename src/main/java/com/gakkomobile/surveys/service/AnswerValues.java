package com.gakkomobile.surveys.service;

import com.gakkomobile.surveys.exception.SurveyValidationException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

final class AnswerValues {

    private AnswerValues() {
    }

    static boolean isEmpty(Object answerValue) {

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

    static List<String> selectedOptions(Object answerValue) {

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

    static int scaleValue(Object answerValue) {

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
}
