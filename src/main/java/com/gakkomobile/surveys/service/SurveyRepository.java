package com.gakkomobile.surveys.service;

import com.gakkomobile.surveys.domain.SurveyEntity;
import com.gakkomobile.surveys.domain.SurveyStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
interface SurveyRepository extends JpaRepository<SurveyEntity, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT survey FROM SurveyEntity survey WHERE survey.id = :surveyIdentifier")
    Optional<SurveyEntity> findByIdForUpdate(@Param("surveyIdentifier") UUID surveyIdentifier);

    @Query("""
            SELECT survey FROM SurveyEntity survey
            WHERE survey.status = :status
              AND (survey.opensAt IS NULL OR survey.opensAt <= :currentTime)
              AND (survey.closesAt IS NULL OR survey.closesAt > :currentTime)
            ORDER BY survey.createdAt DESC
            """)
    List<SurveyEntity> findOpenSurveys(
            @Param("status") SurveyStatus status,
            @Param("currentTime") OffsetDateTime currentTime
    );

    List<SurveyEntity> findByStatusAndClosesAtIsNotNull(SurveyStatus status);

    @Modifying(clearAutomatically = true)
    @Query("""
            UPDATE SurveyEntity survey
            SET survey.status = :closedStatus, survey.updatedAt = :currentTime
            WHERE survey.status = :collectingStatus
              AND survey.closesAt IS NOT NULL
              AND survey.closesAt <= :currentTime
            """)
    int closeExpired(
            @Param("closedStatus") SurveyStatus closedStatus,
            @Param("collectingStatus") SurveyStatus collectingStatus,
            @Param("currentTime") OffsetDateTime currentTime
    );
}
