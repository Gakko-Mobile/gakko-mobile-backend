package com.gakkomobile.surveys.service;

import com.gakkomobile.surveys.domain.QuestionType;
import com.gakkomobile.surveys.domain.SkipRule;
import com.gakkomobile.surveys.domain.SurveyQuestion;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Component
class SkipLogicEvaluator {

    Set<String> hiddenQuestionIdentifiers(List<SurveyQuestion> questions, Map<String, Object> answers) {

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

        if (AnswerValues.isEmpty(answerValue) || Objects.isNull(triggerOptionId)) {
            return false;
        }
        if (question.type() == QuestionType.MULTIPLE) {
            return AnswerValues.selectedOptions(answerValue).contains(triggerOptionId);
        }
        if (question.type() == QuestionType.SCALE) {
            return String.valueOf(AnswerValues.scaleValue(answerValue)).equals(triggerOptionId.trim());
        }
        return String.valueOf(answerValue).equals(triggerOptionId);
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
