package com.gakkomobile.surveys.service;

import com.gakkomobile.surveys.domain.QuestionType;
import com.gakkomobile.surveys.domain.SkipRule;
import com.gakkomobile.surveys.domain.SurveyQuestion;
import com.gakkomobile.surveys.exception.SurveyValidationException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SurveyQuestionDefinitionValidatorTest {

    private final SurveyQuestionDefinitionValidator validator = new SurveyQuestionDefinitionValidator();

    @Test
    void validate_shouldAcceptChoiceScaleAndForwardSkipRules() {

        List<SurveyQuestion> questions = List.of(
                question("q1", QuestionType.SINGLE, List.of("A", "B"), null, null, List.of(new SkipRule("A", "q3"))),
                question("q2", QuestionType.TEXT, List.of(), null, null, List.of()),
                question("q3", QuestionType.SCALE, List.of(), 1, 5, List.of(new SkipRule("3", "q4"))),
                question("q4", QuestionType.TEXT, List.of(), null, null, List.of())
        );

        assertThatCode(() -> validator.validate(questions)).doesNotThrowAnyException();
    }

    @Test
    void validate_shouldRejectDuplicateIdentifiersAndBlankChoiceOptions() {

        assertThatThrownBy(() -> validator.validate(List.of(
                question("q1", QuestionType.TEXT, List.of(), null, null, List.of()),
                question("q1", QuestionType.TEXT, List.of(), null, null, List.of())
        )))
                .isInstanceOf(SurveyValidationException.class)
                .hasMessage("Question identifiers must be unique");

        assertThatThrownBy(() -> validator.validate(List.of(
                question("q1", QuestionType.MULTIPLE, List.of("A", " "), null, null, List.of())
        )))
                .isInstanceOf(SurveyValidationException.class)
                .hasMessage("Choice options cannot be blank");
    }

    @Test
    void validate_shouldRejectInvalidChoiceAndScaleShapes() {

        assertThatThrownBy(() -> validator.validate(List.of(
                question("q1", QuestionType.SINGLE, List.of("Only"), null, null, List.of())
        )))
                .isInstanceOf(SurveyValidationException.class)
                .hasMessage("Choice questions require at least two options");

        assertThatThrownBy(() -> validator.validate(List.of(
                question("q1", QuestionType.SINGLE, List.of("A", "A"), null, null, List.of())
        )))
                .isInstanceOf(SurveyValidationException.class)
                .hasMessage("Choice options must be unique");

        assertThatThrownBy(() -> validator.validate(List.of(
                question("q1", QuestionType.SCALE, List.of(), 5, 5, List.of())
        )))
                .isInstanceOf(SurveyValidationException.class)
                .hasMessage("Scale questions require a minimum lower than the maximum");
    }

    @Test
    void validate_shouldRejectSkipRulesThatDoNotMoveForwardToAMatchingTrigger() {

        SurveyQuestion backward = question("q2", QuestionType.SINGLE, List.of("A", "B"), null, null, List.of(new SkipRule("A", "q1")));
        SurveyQuestion unknownTarget = question("q1", QuestionType.SINGLE, List.of("A", "B"), null, null, List.of(new SkipRule("A", "missing")));
        SurveyQuestion unknownTrigger = question("q1", QuestionType.SINGLE, List.of("A", "B"), null, null, List.of(new SkipRule("C", "q2")));
        SurveyQuestion following = question("q2", QuestionType.TEXT, List.of(), null, null, List.of());

        assertThatThrownBy(() -> validator.validate(List.of(question("q1", QuestionType.TEXT, List.of(), null, null, List.of()), backward)))
                .isInstanceOf(SurveyValidationException.class)
                .hasMessage("Skip rules can only move forward");
        assertThatThrownBy(() -> validator.validate(List.of(unknownTarget, following)))
                .isInstanceOf(SurveyValidationException.class)
                .hasMessage("Skip rule target does not match a question");
        assertThatThrownBy(() -> validator.validate(List.of(unknownTrigger, following)))
                .isInstanceOf(SurveyValidationException.class)
                .hasMessage("Skip rule trigger does not match the question");
    }

    private SurveyQuestion question(
            String id,
            QuestionType type,
            List<String> options,
            Integer scaleMin,
            Integer scaleMax,
            List<SkipRule> skipRules
    ) {

        return new SurveyQuestion(id, type, "Question", true, options, scaleMin, scaleMax, null, null, skipRules);
    }
}
