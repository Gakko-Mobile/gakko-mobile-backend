package com.gakkomobile.messages.service;

import com.gakkomobile.common.event.EventActionType;
import com.gakkomobile.common.event.EventAggregationType;
import com.gakkomobile.notifications.NotificationCategory;
import com.gakkomobile.notifications.NotificationFacade;
import com.gakkomobile.notifications.dto.NotificationDTO;
import com.gakkomobile.notifications.outbox.NotificationTargetType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
@RequiredArgsConstructor
class MessageNotificationPublisher {

    static final String TARGET_SCREEN = "messages-screen";
    private static final int PREVIEW_LENGTH = 180;

    private final NotificationFacade notificationFacade;
    private final ChatThreadParticipantRepository participantRepository;

    void publishDelivered(ChatMessageEntity message) {

        for (ChatThreadParticipantEntity participant : participantRepository.findByThreadId(message.getThreadId())) {
            if (Objects.equals(participant.getUserId(), message.getSenderId())) {
                continue;
            }
            notificationFacade.sendNotification(NotificationDTO.builder()
                    .aggregateType(EventAggregationType.MESSAGE)
                    .aggregateId(message.getId().toString())
                    .eventType(EventActionType.MESSAGE_CREATED)
                    .targetType(NotificationTargetType.USER)
                    .targetId(participant.getUserId().toString())
                    .title("New message")
                    .body(preview(message.getContent()))
                    .category(NotificationCategory.COMMUNICATION.name())
                    .targetScreen(TARGET_SCREEN)
                    .build());
        }
    }

    private static String preview(String content) {

        if (content.length() <= PREVIEW_LENGTH) {
            return content;
        }
        return content.substring(0, PREVIEW_LENGTH - 3) + "...";
    }
}
