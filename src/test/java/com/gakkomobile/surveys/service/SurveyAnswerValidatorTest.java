package com.gakkomobile.surveys.service;

import com.gakkomobile.surveys.domain.QuestionType;
import com.gakkomobile.surveys.domain.SkipRule;
import com.gakkomobile.surveys.domain.SurveyQuestion;
import com.gakkomobile.surveys.dto.AnswerDTO;
import com.gakkomobile.surveys.exception.SurveyValidationException;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SurveyAnswerValidatorTest {

    private final SurveyAnswerValidator validator = new SurveyAnswerValidator(
            new SkipLogicEvaluator(),
            new AnswerValueValidator()
    );

    @Test
    void toAnswerMap_shouldRejectARepeatedQuestion() {

        assertThatThrownBy(() -> validator.toAnswerMap(List.of(
                new AnswerDTO("q1", "A"),
                new AnswerDTO("q1", "B")
        )))
                .isInstanceOf(SurveyValidationException.class)
                .hasMessage("Each question can be answered only once");
    }

    @Test
    void validate_shouldRejectUnknownQuestionsAndMissingRequiredAnswers() {

        List<SurveyQuestion> questions = List.of(textQuestion("q1", true));

        assertThatThrownBy(() -> validator.validate(questions, Map.of("other", "text")))
                .isInstanceOf(SurveyValidationException.class)
                .hasMessage("Answer does not match a survey question");
        assertThatThrownBy(() -> validator.validate(questions, Map.of()))
                .isInstanceOf(SurveyValidationException.class)
                .hasMessage("A required question is missing an answer");
    }

    @Test
    void validate_shouldSkipRequiredQuestionsHiddenBySkipLogic() {

        List<SurveyQuestion> questions = List.of(
                new SurveyQuestion("q1", QuestionType.SINGLE, "Choice", true, List.of("A", "B"), null, null, null, null,
                        List.of(new SkipRule("A", "q3"))),
                textQuestion("q2", true),
                textQuestion("q3", true)
        );

        assertThatCode(() -> validator.validate(questions, Map.of("q1", "A", "q3", "done"))).doesNotThrowAnyException();
        assertThatThrownBy(() -> validator.validate(questions, Map.of("q1", "B", "q3", "done")))
                .isInstanceOf(SurveyValidationException.class)
                .hasMessage("A required question is missing an answer");
    }

    @Test
    void validate_shouldCheckTheShapeOfEachAnswerType() {

        SurveyQuestion single = new SurveyQuestion("single", QuestionType.SINGLE, "Choice", true, List.of("A", "B"), null, null, null, null, List.of());
        SurveyQuestion multiple = new SurveyQuestion("multiple", QuestionType.MULTIPLE, "Choice", true, List.of("A", "B"), null, null, null, null, List.of());
        SurveyQuestion text = textQuestion("text", true);
        SurveyQuestion scale = new SurveyQuestion("scale", QuestionType.SCALE, "Scale", true, List.of(), 1, 3, null, null, List.of());

        assertThatThrownBy(() -> validator.validate(List.of(single), Map.of("single", "C")))
                .hasMessage("Single choice answer must be one of the options");
        assertThatThrownBy(() -> validator.validate(List.of(multiple), Map.of("multiple", "A")))
                .hasMessage("Multiple choice answer must be a list of options");
        assertThatThrownBy(() -> validator.validate(List.of(multiple), Map.of("multiple", List.of("C"))))
                .hasMessage("Multiple choice answer must use the provided options");
        assertThatThrownBy(() -> validator.validate(List.of(text), Map.of("text", 1)))
                .hasMessage("Text answer must be text");
        assertThatThrownBy(() -> validator.validate(List.of(scale), Map.of("scale", 4)))
                .hasMessage("Scale answer is outside the configured range");
        assertThat(validator.toAnswerMap(List.of(new AnswerDTO("scale", "2")))).containsEntry("scale", "2");
        assertThatCode(() -> validator.validate(List.of(scale), Map.of("scale", "2"))).doesNotThrowAnyException();
    }

    private SurveyQuestion textQuestion(String id, boolean required) {

        return new SurveyQuestion(id, QuestionType.TEXT, "Question", required, List.of(), null, null, null, null, List.of());
    }
}
