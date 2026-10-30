package com.gakkomobile.surveys.service;

import com.gakkomobile.common.event.EventActionType;
import com.gakkomobile.common.event.EventAggregationType;
import com.gakkomobile.notifications.NotificationCategory;
import com.gakkomobile.notifications.NotificationFacade;
import com.gakkomobile.notifications.dto.NotificationDTO;
import com.gakkomobile.notifications.outbox.NotificationTargetType;
import com.gakkomobile.surveys.domain.AudienceTargetType;
import com.gakkomobile.surveys.domain.SurveyEntity;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Component
class SurveyNotificationPublisher {

    private static final String SURVEY_SCREEN = "survey-screen";

    private final NotificationFacade notificationFacade;
    private final DatabaseStudentAudienceLookup audienceLookup;

    SurveyNotificationPublisher(
            NotificationFacade notificationFacade,
            DatabaseStudentAudienceLookup audienceLookup
    ) {

        this.notificationFacade = notificationFacade;
        this.audienceLookup = audienceLookup;
    }

    void publishOpened(SurveyEntity survey) {

        List<NotificationDTO> notifications = openedNotifications(survey);
        for (NotificationDTO notification : notifications) {
            notificationFacade.sendNotification(notification);
        }
    }

    void publishReminders(SurveyEntity survey, List<UUID> recipientIdentifiers) {

        for (UUID recipientIdentifier : recipientIdentifiers) {
            notificationFacade.sendNotification(NotificationDTO.builder()
                    .aggregateType(EventAggregationType.SURVEY)
                    .aggregateId(survey.getId().toString())
                    .eventType(EventActionType.SURVEY_REMINDER)
                    .targetType(NotificationTargetType.USER)
                    .targetId(recipientIdentifier.toString())
                    .title("Survey reminder")
                    .body(survey.getTitle())
                    .category(NotificationCategory.EVENTS_STUDENT.name())
                    .targetScreen(SURVEY_SCREEN)
                    .build());
        }
    }

    private List<NotificationDTO> openedNotifications(SurveyEntity survey) {

        if (survey.getTargetType() == AudienceTargetType.VIA_LINK) {
            return List.of();
        }
        if (survey.getTargetType() == AudienceTargetType.ALL) {
            return List.of(openedNotification(survey, NotificationTargetType.ALL_STUDENTS, null));
        }
        List<String> targetIds = targetIdsForOpenedNotification(survey);
        List<NotificationDTO> notifications = new ArrayList<>();
        for (String targetId : targetIds) {
            if (Objects.isNull(targetId) || targetId.isBlank()) {
                continue;
            }
            notifications.add(openedNotification(survey, NotificationTargetType.GROUP, targetId));
        }
        return notifications;
    }

    private List<String> targetIdsForOpenedNotification(SurveyEntity survey) {

        if (survey.getTargetType() == AudienceTargetType.COURSE) {
            return audienceLookup.findGroupIdentifiersForCourseTargets(survey.getTargetIds());
        }
        if (Objects.isNull(survey.getTargetIds())) {
            return List.of();
        }
        return survey.getTargetIds();
    }

    private NotificationDTO openedNotification(
            SurveyEntity survey,
            NotificationTargetType targetType,
            String targetId
    ) {

        return NotificationDTO.builder()
                .aggregateType(EventAggregationType.SURVEY)
                .aggregateId(survey.getId().toString())
                .eventType(EventActionType.SURVEY_CREATED)
                .targetType(targetType)
                .targetId(targetId)
                .title("New survey")
                .body(survey.getTitle())
                .category(NotificationCategory.EVENTS_STUDENT.name())
                .targetScreen(SURVEY_SCREEN)
                .build();
    }
}
