package com.gakkomobile.surveys.service;

import com.gakkomobile.surveys.domain.AudienceTargetType;
import com.gakkomobile.surveys.domain.SurveyEntity;
import com.gakkomobile.surveys.domain.SurveyStatus;
import com.gakkomobile.surveys.dto.SurveyResponseDTO;
import com.gakkomobile.surveys.exception.SurveyNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class StudentSurveyQueryService {

    private final SurveyRepository surveyRepository;
    private final SurveyCompletionRepository surveyCompletionRepository;
    private final DatabaseStudentAudienceLookup audienceLookup;
    private final SurveyAudienceMatcher surveyAudienceMatcher;
    private final SurveyPresentation surveyPresentation;
    private final SurveyLookup surveyLookup;

    @Transactional(readOnly = true)
    public Page<SurveyResponseDTO> getPendingSurveys(UUID userIdentifier, Pageable pageable) {

        OffsetDateTime currentTime = OffsetDateTime.now();
        Set<UUID> completedSurveyIdentifiers = new HashSet<>(
                surveyCompletionRepository.findSurveyIdentifiersByUserIdentifier(userIdentifier));
        Set<String> groupTokens = audienceLookup.findGroupTokens(userIdentifier);
        Set<String> subjectTokens = audienceLookup.findSubjectTokens(userIdentifier);

        List<SurveyResponseDTO> pendingSurveys = new ArrayList<>();
        for (SurveyEntity survey : surveyRepository.findOpenSurveys(SurveyStatus.COLLECTING, currentTime)) {
            if (survey.getTargetType() == AudienceTargetType.VIA_LINK) {
                continue;
            }
            if (completedSurveyIdentifiers.contains(survey.getId())) {
                continue;
            }
            if (!surveyAudienceMatcher.matches(survey, groupTokens, subjectTokens)) {
                continue;
            }
            pendingSurveys.add(surveyPresentation.toStudentResponse(survey, userIdentifier, false, false));
        }
        return toPage(pendingSurveys, pageable);
    }

    @Transactional(readOnly = true)
    public Page<SurveyResponseDTO> getCompletedSurveys(UUID userIdentifier, Pageable pageable) {

        List<UUID> completedSurveyIdentifiers = surveyCompletionRepository.findSurveyIdentifiersByUserIdentifier(userIdentifier);
        List<SurveyEntity> surveys = new ArrayList<>(surveyRepository.findAllById(completedSurveyIdentifiers));
        surveys.sort(Comparator.comparing(SurveyEntity::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())));

        List<SurveyResponseDTO> completedSurveys = new ArrayList<>();
        for (SurveyEntity survey : surveys) {
            completedSurveys.add(surveyPresentation.toStudentResponse(survey, userIdentifier, true, false));
        }
        return toPage(completedSurveys, pageable);
    }

    @Transactional(readOnly = true)
    public SurveyResponseDTO getSurveyForStudent(UUID surveyIdentifier, UUID userIdentifier) {

        SurveyEntity survey = surveyLookup.find(surveyIdentifier);
        boolean completed = surveyCompletionRepository.existsBySurveyIdAndUserId(surveyIdentifier, userIdentifier);
        if (!completed && !surveyAudienceMatcher.matches(
                survey,
                audienceLookup.findGroupTokens(userIdentifier),
                audienceLookup.findSubjectTokens(userIdentifier)
        )) {
            throw new SurveyNotFoundException(surveyIdentifier);
        }
        return surveyPresentation.toStudentResponse(survey, userIdentifier, completed, true);
    }

    private Page<SurveyResponseDTO> toPage(List<SurveyResponseDTO> surveys, Pageable pageable) {

        int start = (int) Math.min(pageable.getOffset(), surveys.size());
        int end = Math.min(start + pageable.getPageSize(), surveys.size());
        List<SurveyResponseDTO> content = new ArrayList<>(surveys.subList(start, end));
        return new PageImpl<>(content, pageable, surveys.size());
    }
}
