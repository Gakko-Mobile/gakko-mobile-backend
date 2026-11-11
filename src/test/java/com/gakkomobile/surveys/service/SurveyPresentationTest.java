package com.gakkomobile.surveys.service;

import com.gakkomobile.surveys.domain.QuestionType;
import com.gakkomobile.surveys.domain.SurveyEntity;
import com.gakkomobile.surveys.domain.SurveyQuestion;
import com.gakkomobile.surveys.dto.SurveyQuestionDTO;
import com.gakkomobile.surveys.dto.SurveyResponseDTO;
import com.gakkomobile.user.UserEntity;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class SurveyPresentationTest {

    private final SurveyMapper surveyMapper = new SurveyMapperImpl();
    private final SurveyPresentation presentation = new SurveyPresentation(
            new SurveyPresentationMapperImpl(),
            surveyMapper
    );

    @Test
    void toStudentResponse_shouldDescribePrivacyTimeAndFreshness() {

        SurveyEntity anonymous = survey(true);
        anonymous.setUpdatedAt(OffsetDateTime.now());
        SurveyResponseDTO fresh = presentation.toStudentResponse(anonymous, UUID.randomUUID(), false, false);

        SurveyEntity named = survey(false);
        named.setCreator(creator("Ada", "Lovelace"));
        named.setUpdatedAt(OffsetDateTime.now().minusHours(72));
        named.setQuestions(List.of());
        SurveyResponseDTO stale = presentation.toStudentResponse(named, UUID.randomUUID(), true, false);

        assertThat(fresh.privacyStatus()).isEqualTo("Anonymous");
        assertThat(fresh.authorName()).isEqualTo("Unknown");
        assertThat(fresh.estimatedTimeLeft()).isEqualTo("~2 MIN LEFT");
        assertThat(fresh.isNew()).isTrue();
        assertThat(fresh.isCompleted()).isFalse();
        assertThat(fresh.questions()).isEmpty();
        assertThat(stale.privacyStatus()).isEqualTo("Public");
        assertThat(stale.authorName()).isEqualTo("Ada Lovelace");
        assertThat(stale.estimatedTimeLeft()).isEqualTo("~1 MIN LEFT");
        assertThat(stale.isNew()).isFalse();
        assertThat(stale.isCompleted()).isTrue();
    }

    @Test
    void toStudentResponse_shouldKeepQuestionOrderStableForTheSameStudent() {

        SurveyEntity survey = survey(true);
        survey.setRandomizeQuestionOrder(true);
        survey.setQuestions(List.of(
                question("q1"),
                question("q2"),
                question("q3"),
                question("q4")
        ));
        UUID studentIdentifier = UUID.fromString("00000000-0000-0000-0000-000000000002");

        SurveyResponseDTO first = presentation.toStudentResponse(survey, studentIdentifier, false, true);
        SurveyResponseDTO second = presentation.toStudentResponse(survey, studentIdentifier, false, true);
        survey.setRandomizeQuestionOrder(false);
        SurveyResponseDTO original = presentation.toStudentResponse(survey, studentIdentifier, false, true);

        assertThat(first.questions()).extracting(SurveyQuestionDTO::id)
                .containsExactlyElementsOf(second.questions().stream().map(SurveyQuestionDTO::id).toList());
        assertThat(first.questions()).extracting(SurveyQuestionDTO::id)
                .isNotEqualTo(List.of("q1", "q2", "q3", "q4"));
        assertThat(original.questions()).extracting(SurveyQuestionDTO::id)
                .containsExactly("q1", "q2", "q3", "q4");
    }

    @Test
    void toAdminDetails_shouldExposeTheCreatorAndADetachedTargetList() {

        SurveyEntity survey = survey(true);
        survey.setCreator(creator("Grace", "Hopper"));
        survey.setTargetIds(new java.util.ArrayList<>(List.of("CS-1")));

        var details = presentation.toAdminDetails(survey);
        survey.getTargetIds().add("CS-2");

        assertThat(details.creatorId()).isEqualTo(survey.getCreator().getId());
        assertThat(details.authorName()).isEqualTo("Grace Hopper");
        assertThat(details.targetIds()).containsExactly("CS-1");
        assertThat(details.questions()).extracting(SurveyQuestionDTO::id).containsExactly("q1", "q2");
    }

    private SurveyEntity survey(boolean anonymous) {

        SurveyEntity survey = new SurveyEntity();
        survey.setId(UUID.fromString("00000000-0000-0000-0000-000000000001"));
        survey.setTitle("Feedback");
        survey.setAnonymous(anonymous);
        survey.setQuestions(List.of(question("q1"), question("q2")));
        return survey;
    }

    private SurveyQuestion question(String id) {

        return new SurveyQuestion(id, QuestionType.TEXT, "Question", false, List.of(), null, null, null, null, List.of());
    }

    private UserEntity creator(String firstName, String lastName) {

        UserEntity creator = new UserEntity();
        creator.setId(UUID.fromString("00000000-0000-0000-0000-000000000099"));
        creator.setFirstName(firstName);
        creator.setLastName(lastName);
        return creator;
    }
}
