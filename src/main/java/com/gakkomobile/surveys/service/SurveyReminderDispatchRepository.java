package com.gakkomobile.surveys.service;

import com.gakkomobile.surveys.domain.SurveyReminderDispatchEntity;
import com.gakkomobile.surveys.domain.SurveyReminderDispatchKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.UUID;

@Repository
interface SurveyReminderDispatchRepository extends JpaRepository<SurveyReminderDispatchEntity, SurveyReminderDispatchKey> {

    @Query("""
            SELECT COUNT(dispatch) FROM SurveyReminderDispatchEntity dispatch
            WHERE dispatch.id.surveyId = :surveyIdentifier
              AND dispatch.id.intervalDays = :intervalDays
            """)
    long countDispatches(
            @Param("surveyIdentifier") UUID surveyIdentifier,
            @Param("intervalDays") BigDecimal intervalDays
    );

    default boolean alreadyDispatched(UUID surveyIdentifier, BigDecimal intervalDays) {

        return countDispatches(surveyIdentifier, intervalDays) > 0;
    }
}
