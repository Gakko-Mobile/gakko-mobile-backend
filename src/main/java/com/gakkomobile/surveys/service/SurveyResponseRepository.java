package com.gakkomobile.surveys.service;

import com.gakkomobile.surveys.domain.SurveyResponseEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
interface SurveyResponseRepository extends JpaRepository<SurveyResponseEntity, UUID> {

    Optional<SurveyResponseEntity> findBySurveyIdAndUserId(UUID surveyId, UUID userId);

    List<SurveyResponseEntity> findBySurveyId(UUID surveyId);

    long countBySurveyId(UUID surveyId);
}
