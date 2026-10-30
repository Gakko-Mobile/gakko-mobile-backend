package com.gakkomobile.surveys.service;

import com.gakkomobile.surveys.domain.SurveyEntity;
import com.gakkomobile.surveys.domain.SurveyQuestion;
import com.gakkomobile.surveys.dto.AdminSurveyDetailsDTO;
import com.gakkomobile.surveys.dto.SurveyQuestionDTO;
import com.gakkomobile.surveys.dto.SurveyResponseDTO;
import com.gakkomobile.user.UserEntity;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.UUID;

@Component
class SurveyPresentation {

    private static final Duration NEW_SURVEY_WINDOW = Duration.ofHours(48);

    private final SurveyMapper surveyMapper;

    SurveyPresentation(SurveyMapper surveyMapper) {

        this.surveyMapper = surveyMapper;
    }

    AdminSurveyDetailsDTO toAdminDetails(SurveyEntity survey) {

        UserEntity creator = survey.getCreator();
        UUID creatorIdentifier = Objects.isNull(creator) ? null : creator.getId();
        return AdminSurveyDetailsDTO.builder()
                .id(survey.getId())
                .creatorId(creatorIdentifier)
                .authorName(authorName(creator))
                .title(survey.getTitle())
                .description(survey.getDescription())
                .questions(surveyMapper.toApiQuestions(survey.getQuestions()))
                .status(survey.getStatus())
                .targetType(survey.getTargetType())
                .targetIds(copyStrings(survey.getTargetIds()))
                .anonymous(survey.isAnonymous())
                .allowResubmission(survey.isAllowResubmission())
                .requireLogin(survey.isRequireLogin())
                .showResultsToStudents(survey.isShowResultsToStudents())
                .randomizeQuestionOrder(survey.isRandomizeQuestionOrder())
                .responseLimit(survey.getResponseLimit())
                .confirmationMessage(survey.getConfirmationMessage())
                .opensAt(survey.getOpensAt())
                .closesAt(survey.getClosesAt())
                .reminderIntervals(copyDoubles(survey.getReminderIntervals()))
                .createdAt(survey.getCreatedAt())
                .updatedAt(survey.getUpdatedAt())
                .build();
    }

    SurveyResponseDTO toStudentResponse(
            SurveyEntity survey,
            UUID userIdentifier,
            boolean completed,
            boolean includeQuestions
    ) {

        List<SurveyQuestionDTO> questions = List.of();
        if (includeQuestions) {
            questions = surveyMapper.toApiQuestions(orderedQuestions(survey, userIdentifier));
        }
        int questionCount = Objects.isNull(survey.getQuestions()) ? 0 : survey.getQuestions().size();
        return SurveyResponseDTO.builder()
                .id(survey.getId())
                .title(survey.getTitle())
                .description(survey.getDescription())
                .authorName(authorName(survey.getCreator()))
                .deadline(survey.getClosesAt())
                .questionCount(questionCount)
                .privacyStatus(survey.isAnonymous() ? "Anonymous" : "Public")
                .estimatedTimeLeft(estimatedTimeLeft(questionCount))
                .isNew(isNew(survey))
                .isCompleted(completed)
                .status(survey.getStatus())
                .showResultsToStudents(survey.isShowResultsToStudents())
                .allowResubmission(survey.isAllowResubmission())
                .requireLogin(survey.isRequireLogin())
                .confirmationMessage(survey.getConfirmationMessage())
                .questions(questions)
                .build();
    }

    private List<SurveyQuestion> orderedQuestions(SurveyEntity survey, UUID userIdentifier) {

        List<SurveyQuestion> questions = new ArrayList<>();
        if (!Objects.isNull(survey.getQuestions())) {
            questions.addAll(survey.getQuestions());
        }
        if (!survey.isRandomizeQuestionOrder() || questions.size() < 2 || Objects.isNull(userIdentifier)) {
            return questions;
        }
        long seed = survey.getId().getMostSignificantBits() ^ userIdentifier.getLeastSignificantBits();
        Collections.shuffle(questions, new Random(seed));
        return questions;
    }

    private String authorName(UserEntity creator) {

        if (Objects.isNull(creator)) {
            return "Unknown";
        }
        return creator.getFirstName() + " " + creator.getLastName();
    }

    private String estimatedTimeLeft(int questionCount) {

        int minutes = Math.max(questionCount, 1);
        return "~" + minutes + " MIN LEFT";
    }

    private boolean isNew(SurveyEntity survey) {

        if (Objects.isNull(survey.getUpdatedAt())) {
            return false;
        }
        return survey.getUpdatedAt().isAfter(OffsetDateTime.now().minus(NEW_SURVEY_WINDOW));
    }

    private List<String> copyStrings(List<String> values) {

        if (Objects.isNull(values)) {
            return List.of();
        }
        return List.copyOf(values);
    }

    private List<Double> copyDoubles(List<Double> values) {

        if (Objects.isNull(values)) {
            return List.of();
        }
        return List.copyOf(values);
    }
}
