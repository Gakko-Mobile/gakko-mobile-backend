package com.gakkomobile.surveys.service;

import com.gakkomobile.surveys.domain.SurveyQuestion;
import com.gakkomobile.surveys.dto.AnswerDTO;
import com.gakkomobile.surveys.exception.SurveyValidationException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Component
@RequiredArgsConstructor
class SurveyAnswerValidator {

    private final SkipLogicEvaluator skipLogicEvaluator;
    private final AnswerValueValidator answerValueValidator;

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

        Set<String> hiddenQuestionIdentifiers = skipLogicEvaluator.hiddenQuestionIdentifiers(questions, answers);
        for (SurveyQuestion question : questions) {
            if (hiddenQuestionIdentifiers.contains(question.id())) {
                continue;
            }
            Object answerValue = answers.get(question.id());
            if (question.isRequired() && AnswerValues.isEmpty(answerValue)) {
                throw new SurveyValidationException("A required question is missing an answer");
            }
            if (AnswerValues.isEmpty(answerValue)) {
                continue;
            }
            answerValueValidator.validate(question, answerValue);
        }
    }
}
