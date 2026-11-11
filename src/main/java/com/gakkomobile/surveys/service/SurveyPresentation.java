package com.gakkomobile.surveys.service;

import com.gakkomobile.surveys.domain.SurveyEntity;
import com.gakkomobile.surveys.domain.SurveyQuestion;
import com.gakkomobile.surveys.dto.AdminSurveyDetailsDTO;
import com.gakkomobile.surveys.dto.SurveyQuestionDTO;
import com.gakkomobile.surveys.dto.SurveyResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.UUID;

@Component
@RequiredArgsConstructor
class SurveyPresentation {

    private final SurveyPresentationMapper surveyPresentationMapper;
    private final SurveyMapper surveyMapper;

    AdminSurveyDetailsDTO toAdminDetails(SurveyEntity survey) {

        return surveyPresentationMapper.toAdminDetails(survey, surveyMapper.toApiQuestions(survey.getQuestions()));
    }

    SurveyResponseDTO toStudentResponse(SurveyEntity survey, UUID userIdentifier, boolean completed, boolean includeQuestions) {

        List<SurveyQuestionDTO> questions = List.of();
        if (includeQuestions) {
            questions = surveyMapper.toApiQuestions(orderedQuestions(survey, userIdentifier));
        }
        return surveyPresentationMapper.toStudentResponse(survey, completed, questions);
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
}
