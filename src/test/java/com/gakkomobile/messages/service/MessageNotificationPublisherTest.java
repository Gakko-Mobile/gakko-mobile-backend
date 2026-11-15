package com.gakkomobile.messages.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.gakkomobile.common.event.EventActionType;
import com.gakkomobile.common.event.EventAggregationType;
import com.gakkomobile.notifications.NotificationCategory;
import com.gakkomobile.notifications.NotificationFacade;
import com.gakkomobile.notifications.dto.NotificationDTO;
import com.gakkomobile.notifications.outbox.NotificationTargetType;
import com.gakkomobile.utils.DatasetConstants;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

@ExtendWith(MockitoExtension.class)
class MessageNotificationPublisherTest {

    private static final UUID THREAD_ID = DatasetConstants.uuid(1);
    private static final UUID MESSAGE_ID = DatasetConstants.uuid(2);
    private static final UUID SENDER_ID = DatasetConstants.uuid(3);
    private static final UUID RECIPIENT_ID = DatasetConstants.uuid(4);

    @Mock
    private NotificationFacade notificationFacade;
    @Mock
    private ChatThreadParticipantRepository participantRepository;

    @InjectMocks
    private MessageNotificationPublisher publisher;

    @Test
    void publishDelivered_shouldNotifyOtherParticipantsAndSkipTheSender() {

        when(participantRepository.findByThreadId(THREAD_ID)).thenReturn(List.of(
                participant(SENDER_ID),
                participant(RECIPIENT_ID)
        ));

        publisher.publishDelivered(message("a".repeat(181)));

        ArgumentCaptor<NotificationDTO> notification = ArgumentCaptor.forClass(NotificationDTO.class);
        verify(notificationFacade).sendNotification(notification.capture());
        NotificationDTO sent = notification.getValue();
        assertThat(sent.aggregateType()).isEqualTo(EventAggregationType.MESSAGE);
        assertThat(sent.aggregateId()).isEqualTo(MESSAGE_ID.toString());
        assertThat(sent.eventType()).isEqualTo(EventActionType.MESSAGE_CREATED);
        assertThat(sent.targetType()).isEqualTo(NotificationTargetType.USER);
        assertThat(sent.targetId()).isEqualTo(RECIPIENT_ID.toString());
        assertThat(sent.title()).isEqualTo("New message");
        assertThat(sent.body()).isEqualTo("a".repeat(177) + "...");
        assertThat(sent.category()).isEqualTo(NotificationCategory.COMMUNICATION.name());
        assertThat(sent.targetScreen()).isEqualTo(MessageNotificationPublisher.TARGET_SCREEN);
    }

    @Test
    void publishDelivered_shouldKeepAShortPreviewUnchanged() {

        when(participantRepository.findByThreadId(THREAD_ID)).thenReturn(List.of(participant(RECIPIENT_ID)));

        publisher.publishDelivered(message("Hello"));

        ArgumentCaptor<NotificationDTO> notification = ArgumentCaptor.forClass(NotificationDTO.class);
        verify(notificationFacade).sendNotification(notification.capture());
        assertThat(notification.getValue().body()).isEqualTo("Hello");
    }

    @Test
    void publishDelivered_shouldDoNothingWhenTheSenderIsTheOnlyParticipant() {

        when(participantRepository.findByThreadId(THREAD_ID)).thenReturn(List.of(participant(SENDER_ID)));

        publisher.publishDelivered(message("Hello"));

        verify(notificationFacade, never()).sendNotification(org.mockito.ArgumentMatchers.any());
    }

    private static ChatMessageEntity message(String content) {

        ChatMessageEntity message = new ChatMessageEntity();
        message.setId(MESSAGE_ID);
        message.setThreadId(THREAD_ID);
        message.setSenderId(SENDER_ID);
        message.setContent(content);
        return message;
    }

    private static ChatThreadParticipantEntity participant(UUID userId) {

        ChatThreadParticipantEntity participant = new ChatThreadParticipantEntity();
        participant.setThreadId(THREAD_ID);
        participant.setUserId(userId);
        return participant;
    }
}
