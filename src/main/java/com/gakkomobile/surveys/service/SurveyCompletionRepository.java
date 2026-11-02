package com.gakkomobile.surveys.service;

import com.gakkomobile.surveys.domain.SurveyCompletionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
interface SurveyCompletionRepository extends JpaRepository<SurveyCompletionEntity, UUID> {

    Optional<SurveyCompletionEntity> findBySurveyIdAndUserId(UUID surveyId, UUID userId);

    boolean existsBySurveyIdAndUserId(UUID surveyId, UUID userId);

    @Query("SELECT completion.surveyId FROM SurveyCompletionEntity completion WHERE completion.userId = :userIdentifier")
    List<UUID> findSurveyIdentifiersByUserIdentifier(@Param("userIdentifier") UUID userIdentifier);
}
