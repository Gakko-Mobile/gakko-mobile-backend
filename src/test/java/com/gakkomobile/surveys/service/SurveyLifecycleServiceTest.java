package com.gakkomobile.surveys.service;

import com.gakkomobile.surveys.domain.AudienceTargetType;
import com.gakkomobile.surveys.domain.SurveyEntity;
import com.gakkomobile.surveys.domain.SurveyReminderDispatchEntity;
import com.gakkomobile.surveys.domain.SurveyStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.scheduling.annotation.Scheduled;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SurveyLifecycleServiceTest {

    @Mock
    private SurveyRepository surveyRepository;
    @Mock
    private SurveyReminderDispatchRepository surveyReminderDispatchRepository;
    @Mock
    private DatabaseStudentAudienceLookup audienceLookup;
    @Mock
    private SurveyNotificationPublisher surveyNotificationPublisher;

    @InjectMocks
    private SurveyLifecycleService service;

    @Test
    void closeExpiredSurveys_shouldCloseCollectingSurveysThatHaveReachedTheirDeadline() {

        service.closeExpiredSurveys();

        verify(surveyRepository).closeExpired(eq(SurveyStatus.CLOSED), eq(SurveyStatus.COLLECTING), any());
    }

    @Test
    void dispatchDueReminders_shouldNotifyStudentsWhoHaveNotResponded() {

        SurveyEntity survey = reminderSurvey(OffsetDateTime.now().minusHours(1));
        UUID recipientIdentifier = UUID.randomUUID();
        when(surveyRepository.findByStatusAndClosesAtIsNotNull(SurveyStatus.COLLECTING)).thenReturn(List.of(survey));
        when(surveyReminderDispatchRepository.alreadyDispatched(eq(survey.getId()), any())).thenReturn(false);
        when(audienceLookup.findUsersWithoutCompletion(survey.getId(), AudienceTargetType.ALL, survey.getTargetIds()))
                .thenReturn(List.of(recipientIdentifier));

        service.dispatchDueReminders();

        ArgumentCaptor<SurveyReminderDispatchEntity> dispatchCaptor = ArgumentCaptor.forClass(SurveyReminderDispatchEntity.class);
        verify(surveyReminderDispatchRepository).save(dispatchCaptor.capture());
        assertThat(dispatchCaptor.getValue().getId().getSurveyId()).isEqualTo(survey.getId());
        assertThat(dispatchCaptor.getValue().getId().getIntervalDays()).isEqualByComparingTo(new BigDecimal("1.0000"));
        verify(surveyNotificationPublisher).publishReminders(survey, List.of(recipientIdentifier));
    }

    @Test
    void dispatchDueReminders_shouldWaitUntilTheReminderMomentAndSkipDuplicates() {

        SurveyEntity upcoming = reminderSurvey(OffsetDateTime.now().plusDays(10));
        when(surveyRepository.findByStatusAndClosesAtIsNotNull(SurveyStatus.COLLECTING)).thenReturn(List.of(upcoming));
        when(surveyReminderDispatchRepository.alreadyDispatched(eq(upcoming.getId()), any())).thenReturn(false);

        service.dispatchDueReminders();

        verify(surveyReminderDispatchRepository, never()).save(any());
        verify(surveyNotificationPublisher, never()).publishReminders(any(), any());

        SurveyEntity dueAgain = reminderSurvey(OffsetDateTime.now().minusDays(1));
        when(surveyRepository.findByStatusAndClosesAtIsNotNull(SurveyStatus.COLLECTING)).thenReturn(List.of(dueAgain));
        when(surveyReminderDispatchRepository.alreadyDispatched(eq(dueAgain.getId()), any())).thenReturn(true);

        service.dispatchDueReminders();

        verify(surveyNotificationPublisher, never()).publishReminders(any(), any());
    }

    @Test
    void runLifecycle_shouldReadItsDelaysFromConfiguration() throws NoSuchMethodException {

        Scheduled scheduled = SurveyLifecycleScheduler.class.getDeclaredMethod("runLifecycle").getAnnotation(Scheduled.class);

        assertThat(scheduled.initialDelayString()).isEqualTo("${survey.lifecycle.initial-delay}");
        assertThat(scheduled.fixedDelayString()).isEqualTo("${survey.lifecycle.fixed-delay}");
        assertThat(scheduled.initialDelay()).isEqualTo(-1L);
        assertThat(scheduled.fixedDelay()).isEqualTo(-1L);
    }

    private SurveyEntity reminderSurvey(OffsetDateTime closesAt) {

        SurveyEntity survey = new SurveyEntity();
        survey.setId(UUID.randomUUID());
        survey.setStatus(SurveyStatus.COLLECTING);
        survey.setTargetType(AudienceTargetType.ALL);
        survey.setClosesAt(closesAt);
        survey.setReminderIntervals(List.of(1.0));
        return survey;
    }
}
