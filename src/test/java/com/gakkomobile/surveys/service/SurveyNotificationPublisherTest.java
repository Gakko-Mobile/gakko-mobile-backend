package com.gakkomobile.surveys.service;

import com.gakkomobile.notifications.NotificationFacade;
import com.gakkomobile.notifications.dto.NotificationDTO;
import com.gakkomobile.notifications.outbox.NotificationTargetType;
import com.gakkomobile.surveys.domain.AudienceTargetType;
import com.gakkomobile.surveys.domain.SurveyEntity;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SurveyNotificationPublisherTest {

    @Mock
    private NotificationFacade notificationFacade;
    @Mock
    private DatabaseStudentAudienceLookup audienceLookup;

    @InjectMocks
    private SurveyNotificationPublisher publisher;

    @Test
    void publishOpened_shouldSkipLinkOnlySurveysAndBroadcastToEveryStudent() {

        SurveyEntity linkOnly = survey(AudienceTargetType.VIA_LINK);
        publisher.publishOpened(linkOnly);
        verify(notificationFacade, never()).sendNotification(org.mockito.ArgumentMatchers.any());

        SurveyEntity everyone = survey(AudienceTargetType.ALL);
        publisher.publishOpened(everyone);

        ArgumentCaptor<NotificationDTO> notificationCaptor = ArgumentCaptor.forClass(NotificationDTO.class);
        verify(notificationFacade).sendNotification(notificationCaptor.capture());
        assertThat(notificationCaptor.getValue().targetType()).isEqualTo(NotificationTargetType.ALL_STUDENTS);
        assertThat(notificationCaptor.getValue().targetId()).isNull();
        assertThat(notificationCaptor.getValue().aggregateId()).isEqualTo(everyone.getId().toString());
    }

    @Test
    void publishOpened_shouldResolveCourseTargetsToGroups() {

        SurveyEntity survey = survey(AudienceTargetType.COURSE);
        survey.setTargetIds(List.of("course-1"));
        when(audienceLookup.findGroupIdentifiersForCourseTargets(survey.getTargetIds())).thenReturn(List.of("group-1", " "));

        publisher.publishOpened(survey);

        ArgumentCaptor<NotificationDTO> notificationCaptor = ArgumentCaptor.forClass(NotificationDTO.class);
        verify(notificationFacade).sendNotification(notificationCaptor.capture());
        assertThat(notificationCaptor.getValue().targetType()).isEqualTo(NotificationTargetType.GROUP);
        assertThat(notificationCaptor.getValue().targetId()).isEqualTo("group-1");
    }

    private SurveyEntity survey(AudienceTargetType targetType) {

        SurveyEntity survey = new SurveyEntity();
        survey.setId(UUID.randomUUID());
        survey.setTitle("Feedback");
        survey.setTargetType(targetType);
        return survey;
    }
}
