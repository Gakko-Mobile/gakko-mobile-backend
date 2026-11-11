package com.gakkomobile.surveys.service;

import com.gakkomobile.surveys.domain.SurveyEntity;
import com.gakkomobile.surveys.domain.SurveyReminderDispatchEntity;
import com.gakkomobile.surveys.domain.SurveyReminderDispatchKey;
import com.gakkomobile.surveys.domain.SurveyStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
class SurveyLifecycleService {

    private final SurveyRepository surveyRepository;
    private final SurveyReminderDispatchRepository surveyReminderDispatchRepository;
    private final DatabaseStudentAudienceLookup audienceLookup;
    private final SurveyNotificationPublisher surveyNotificationPublisher;

    @Transactional
    void closeExpiredSurveys() {

        int closedSurveys = surveyRepository.closeExpired(
                SurveyStatus.CLOSED,
                SurveyStatus.COLLECTING,
                OffsetDateTime.now()
        );
        if (closedSurveys > 0) {
            log.info("Closed {} expired surveys", closedSurveys);
        }
    }

    @Transactional
    void dispatchDueReminders() {

        OffsetDateTime currentTime = OffsetDateTime.now();
        List<SurveyEntity> surveys = surveyRepository.findByStatusAndClosesAtIsNotNull(SurveyStatus.COLLECTING);
        for (SurveyEntity survey : surveys) {
            dispatchSurveyReminders(survey, currentTime);
        }
    }

    private void dispatchSurveyReminders(SurveyEntity survey, OffsetDateTime currentTime) {

        for (BigDecimal intervalDays : reminderIntervals(survey)) {
            if (surveyReminderDispatchRepository.alreadyDispatched(survey.getId(), intervalDays)) {
                continue;
            }
            if (currentTime.isBefore(reminderMoment(survey.getClosesAt(), intervalDays))) {
                continue;
            }
            List<UUID> recipientIdentifiers = audienceLookup.findUsersWithoutCompletion(
                    survey.getId(),
                    survey.getTargetType(),
                    survey.getTargetIds()
            );
            SurveyReminderDispatchEntity dispatch = new SurveyReminderDispatchEntity();
            dispatch.setId(new SurveyReminderDispatchKey(survey.getId(), intervalDays));
            surveyReminderDispatchRepository.save(dispatch);
            surveyNotificationPublisher.publishReminders(survey, recipientIdentifiers);
            log.info(
                    "Survey reminder dispatched: surveyId={}, intervalDays={}, recipientCount={}",
                    survey.getId(),
                    intervalDays,
                    recipientIdentifiers.size()
            );
        }
    }

    private List<BigDecimal> reminderIntervals(SurveyEntity survey) {

        if (Objects.isNull(survey.getReminderIntervals()) || survey.getReminderIntervals().isEmpty()) {
            return List.of();
        }
        List<BigDecimal> intervals = new ArrayList<>();
        for (Double reminderInterval : survey.getReminderIntervals()) {
            if (Objects.isNull(reminderInterval) || reminderInterval <= 0) {
                continue;
            }
            intervals.add(BigDecimal.valueOf(reminderInterval).setScale(4, RoundingMode.HALF_UP));
        }
        return intervals;
    }

    private OffsetDateTime reminderMoment(OffsetDateTime closesAt, BigDecimal intervalDays) {

        long hoursBeforeClose = intervalDays
                .multiply(BigDecimal.valueOf(24))
                .setScale(0, RoundingMode.HALF_UP)
                .longValueExact();
        return closesAt.minusHours(hoursBeforeClose);
    }
}
