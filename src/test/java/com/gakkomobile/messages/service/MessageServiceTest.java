package com.gakkomobile.messages.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.gakkomobile.exception.ResourceNotFoundException;
import com.gakkomobile.messages.domain.MessageStatus;
import com.gakkomobile.messages.domain.ThreadType;
import com.gakkomobile.messages.dto.ChatMessageResponseDTO;
import com.gakkomobile.messages.dto.ChatThreadSummaryResponseDTO;
import com.gakkomobile.messages.dto.CreateThreadRequestDTO;
import com.gakkomobile.messages.dto.SendMessagePayloadDTO;
import com.gakkomobile.user.Role;
import com.gakkomobile.user.UserEntity;
import com.gakkomobile.user.UserRepository;
import com.gakkomobile.utils.DatasetConstants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@ExtendWith(MockitoExtension.class)
class MessageServiceTest {

    private static final ZoneId WARSAW = ZoneId.of("Europe/Warsaw");
    private static final UUID CREATOR_ID = DatasetConstants.uuid(1);
    private static final UUID LECTURER_ID =  DatasetConstants.uuid(2);
    private static final UUID THREAD_ID = DatasetConstants.uuid(3);
    private static final UUID MESSAGE_ID = DatasetConstants.uuid(4);

    @Mock
    private ChatThreadRepository threadRepository;
    @Mock
    private ChatThreadParticipantRepository participantRepository;
    @Mock
    private ChatMessageRepository messageRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private ThreadAccessGuard threadAccessGuard;
    @Mock
    private MessageNotificationPublisher notificationPublisher;

    private MessageService messageService;

    @BeforeEach
    void setUp() {

        messageService = serviceAt(LocalDateTime.of(2026, 11, 11, 10, 0));
    }

    @Test
    void createThread_shouldTrimSubjectAndStoreEachParticipantOnce() {

        when(userRepository.findById(CREATOR_ID)).thenReturn(Optional.of(user(CREATOR_ID, Role.STUDENT, "Ada", "Nowak")));
        when(userRepository.findById(LECTURER_ID)).thenReturn(Optional.of(user(LECTURER_ID, Role.LECTURER, "Jan", "Kowalski")));
        when(threadRepository.save(any(ChatThreadEntity.class))).thenAnswer(invocation -> {
            ChatThreadEntity thread = invocation.getArgument(0);
            thread.setId(THREAD_ID);
            return thread;
        });

        ChatThreadSummaryResponseDTO response = messageService.createThread(
                CREATOR_ID,
                new CreateThreadRequestDTO("  Consultation  ", ThreadType.DIRECT, List.of(LECTURER_ID, CREATOR_ID))
        );

        assertThat(response.threadId()).isEqualTo(THREAD_ID);
        assertThat(response.subject()).isEqualTo("Consultation");
        assertThat(response.type()).isEqualTo(ThreadType.DIRECT);
        assertThat(response.unreadCount()).isZero();

        ArgumentCaptor<ChatThreadParticipantEntity> participants = ArgumentCaptor.forClass(ChatThreadParticipantEntity.class);
        verify(participantRepository, times(2)).save(participants.capture());
        assertThat(participants.getAllValues())
                .extracting(ChatThreadParticipantEntity::getUserId)
                .containsExactly(CREATOR_ID, LECTURER_ID);
        assertThat(participants.getAllValues())
                .extracting(ChatThreadParticipantEntity::getThreadId)
                .containsOnly(THREAD_ID);
    }

    @Test
    void createThread_shouldRejectAnUnknownParticipantBeforeSaving() {

        when(userRepository.findById(CREATOR_ID)).thenReturn(Optional.of(user(CREATOR_ID, Role.STUDENT, "Ada", "Nowak")));
        when(userRepository.findById(LECTURER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> messageService.createThread(
                CREATOR_ID,
                new CreateThreadRequestDTO("Consultation", ThreadType.DIRECT, List.of(LECTURER_ID))
        ))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("User [%s] not found".formatted(LECTURER_ID));
        verify(threadRepository, never()).save(any());
    }

    @Test
    void sendMessage_shouldDeliverAndNotifyDuringWorkingHours() {

        UserEntity sender = user(CREATOR_ID, Role.STUDENT, "Ada", "Nowak");
        ChatThreadEntity thread = thread(THREAD_ID);
        when(userRepository.findById(CREATOR_ID)).thenReturn(Optional.of(sender));
        when(threadRepository.findById(THREAD_ID)).thenReturn(Optional.of(thread));
        when(participantRepository.findOtherParticipantsByRole(THREAD_ID, CREATOR_ID, Role.LECTURER))
                .thenReturn(List.of(LECTURER_ID));
        when(messageRepository.saveAndFlush(any(ChatMessageEntity.class))).thenAnswer(this::assignMessageId);

        ChatMessageResponseDTO response = messageService.sendMessage(
                CREATOR_ID,
                THREAD_ID,
                new SendMessagePayloadDTO("  Hello lecturer  ", false)
        );

        assertThat(response.id()).isEqualTo(MESSAGE_ID);
        assertThat(response.content()).isEqualTo("Hello lecturer");
        assertThat(response.senderName()).isEqualTo("Ada Nowak");
        assertThat(response.isBuffered()).isFalse();
        assertThat(response.status()).isEqualTo(MessageStatus.DELIVERED);
        assertThat(response.scheduledDeliveryAt()).isNull();
        assertThat(thread.getUpdatedAt().toInstant()).isEqualTo(at(2026, 11, 11, 10, 0));

        ArgumentCaptor<ChatMessageEntity> saved = ArgumentCaptor.forClass(ChatMessageEntity.class);
        verify(notificationPublisher).publishDelivered(saved.capture());
        assertThat(saved.getValue().getStatus()).isEqualTo(MessageStatus.DELIVERED);
    }

    @Test
    void sendMessage_shouldBufferANonUrgentStudentMessageToALecturerOutsideWorkingHours() {

        messageService = serviceAt(LocalDateTime.of(2026, 11, 11, 20, 0));
        when(userRepository.findById(CREATOR_ID)).thenReturn(Optional.of(user(CREATOR_ID, Role.STUDENT, "Ada", "Nowak")));
        when(threadRepository.findById(THREAD_ID)).thenReturn(Optional.of(thread(THREAD_ID)));
        when(participantRepository.findOtherParticipantsByRole(THREAD_ID, CREATOR_ID, Role.LECTURER))
                .thenReturn(List.of(LECTURER_ID));
        when(messageRepository.saveAndFlush(any(ChatMessageEntity.class))).thenAnswer(this::assignMessageId);

        ChatMessageResponseDTO response = messageService.sendMessage(
                CREATOR_ID,
                THREAD_ID,
                new SendMessagePayloadDTO("After hours", false)
        );

        assertThat(response.isBuffered()).isTrue();
        assertThat(response.status()).isEqualTo(MessageStatus.BUFFERED);
        assertThat(response.scheduledDeliveryAt()).isEqualTo(at(2026, 11, 12, 8, 0));
        verify(notificationPublisher, never()).publishDelivered(any());
    }

    @Test
    void sendMessage_shouldDeliverAnUrgentMessageOutsideWorkingHours() {

        messageService = serviceAt(LocalDateTime.of(2026, 11, 11, 20, 0));
        when(userRepository.findById(CREATOR_ID)).thenReturn(Optional.of(user(CREATOR_ID, Role.STUDENT, "Ada", "Nowak")));
        when(threadRepository.findById(THREAD_ID)).thenReturn(Optional.of(thread(THREAD_ID)));
        when(participantRepository.findOtherParticipantsByRole(THREAD_ID, CREATOR_ID, Role.LECTURER))
                .thenReturn(List.of(LECTURER_ID));
        when(messageRepository.saveAndFlush(any(ChatMessageEntity.class))).thenAnswer(this::assignMessageId);

        ChatMessageResponseDTO response = messageService.sendMessage(
                CREATOR_ID,
                THREAD_ID,
                new SendMessagePayloadDTO("Urgent", true)
        );

        assertThat(response.isUrgent()).isTrue();
        assertThat(response.isBuffered()).isFalse();
        assertThat(response.status()).isEqualTo(MessageStatus.DELIVERED);
        verify(notificationPublisher).publishDelivered(any(ChatMessageEntity.class));
    }

    @Test
    void sendMessage_shouldRejectAUserWhoIsNotAParticipant() {

        doThrow(new AccessDeniedException("User is not a participant of this thread"))
                .when(threadAccessGuard).requireParticipant(THREAD_ID, CREATOR_ID);

        assertThatThrownBy(() -> messageService.sendMessage(
                CREATOR_ID,
                THREAD_ID,
                new SendMessagePayloadDTO("Nope", false)
        )).isInstanceOf(AccessDeniedException.class);
        verify(messageRepository, never()).saveAndFlush(any());
    }

    @Test
    void sendMessage_shouldFailWhenTheThreadDoesNotExist() {

        when(userRepository.findById(CREATOR_ID)).thenReturn(Optional.of(user(CREATOR_ID, Role.STUDENT, "Ada", "Nowak")));
        when(threadRepository.findById(THREAD_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> messageService.sendMessage(
                CREATOR_ID,
                THREAD_ID,
                new SendMessagePayloadDTO("Hello", false)
        ))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Chat thread [%s]not found".formatted(THREAD_ID));
        verify(messageRepository, never()).saveAndFlush(any());
    }

    @Test
    void listMessages_shouldMapSenderNamesAndSkipMissingSenders() {

        ChatMessageEntity fromStudent = message(MESSAGE_ID, CREATOR_ID, "Hello");
        ChatMessageEntity withoutSender = message(UUID.randomUUID(), null, "System");
        when(messageRepository.findVisible(THREAD_ID, LECTURER_ID, PageRequest.of(0, 20)))
                .thenReturn(new PageImpl<>(List.of(fromStudent, withoutSender)));
        when(userRepository.findAllById(Set.of(CREATOR_ID)))
                .thenReturn(List.of(user(CREATOR_ID, Role.STUDENT, "Ada", "Nowak")));

        Page<ChatMessageResponseDTO> page = messageService.listMessages(LECTURER_ID, THREAD_ID, PageRequest.of(0, 20));

        assertThat(page.getContent()).extracting(ChatMessageResponseDTO::senderName).containsExactly("Ada Nowak", null);
        verify(threadAccessGuard).requireParticipant(THREAD_ID, LECTURER_ID);
    }

    @Test
    void listMessages_shouldNotLoadSendersWhenThePageIsEmpty() {

        when(messageRepository.findVisible(any(), any(), any())).thenReturn(Page.empty());

        Page<ChatMessageResponseDTO> page = messageService.listMessages(CREATOR_ID, THREAD_ID, PageRequest.of(0, 20));

        assertThat(page).isEmpty();
        verify(userRepository, never()).findAllById(any());
    }

    @Test
    void listThreads_shouldMapRepositorySummaries() {

        OffsetDateTime lastMessageAt = OffsetDateTime.ofInstant(at(2026, 11, 11, 10, 0), WARSAW);
        ChatThreadSummaryView view = mock(ChatThreadSummaryView.class);
        when(view.getThreadId()).thenReturn(THREAD_ID);
        when(view.getSubject()).thenReturn("Consultation");
        when(view.getThreadType()).thenReturn(ThreadType.DIRECT.name());
        when(view.getLastMessageAt()).thenReturn(lastMessageAt);
        when(view.getLastMessageContent()).thenReturn("Hello");
        when(view.getUnreadCount()).thenReturn(2L);
        when(threadRepository.findSummariesForUser(CREATOR_ID)).thenReturn(List.of(view));

        assertThat(messageService.listThreads(CREATOR_ID)).containsExactly(new ChatThreadSummaryResponseDTO(
                THREAD_ID,
                "Consultation",
                ThreadType.DIRECT,
                at(2026, 11, 11, 10, 0),
                "Hello",
                2
        ));
    }

    @Test
    void markThreadAsRead_shouldStampMembershipWithTheClock() {

        ChatThreadParticipantEntity membership = new ChatThreadParticipantEntity();
        when(threadAccessGuard.requireMembership(THREAD_ID, CREATOR_ID)).thenReturn(membership);

        messageService.markThreadAsRead(CREATOR_ID, THREAD_ID);

        assertThat(membership.getLastReadAt().toInstant()).isEqualTo(at(2026, 11, 11, 10, 0));
    }

    @Test
    void releaseDueMessages_shouldDeliverEachBufferedMessageAndNotify() {

        ChatMessageEntity first = message(MESSAGE_ID, CREATOR_ID, "One");
        first.setBuffered(true);
        first.setStatus(MessageStatus.BUFFERED);
        ChatMessageEntity second = message(UUID.randomUUID(), CREATOR_ID, "Two");
        second.setBuffered(true);
        second.setStatus(MessageStatus.BUFFERED);
        when(messageRepository.findDueBuffered(any())).thenReturn(List.of(first, second));
        when(messageRepository.saveAndFlush(any(ChatMessageEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        messageService.releaseDueMessages();

        assertThat(first.isBuffered()).isFalse();
        assertThat(second.getStatus()).isEqualTo(MessageStatus.DELIVERED);
        verify(notificationPublisher, times(2)).publishDelivered(any(ChatMessageEntity.class));

        ArgumentCaptor<OffsetDateTime> now = ArgumentCaptor.forClass(OffsetDateTime.class);
        verify(messageRepository).findDueBuffered(now.capture());
        assertThat(now.getValue().toInstant()).isEqualTo(at(2026, 11, 11, 10, 0));
    }

    private MessageService serviceAt(LocalDateTime warsawTime) {

        Clock clock = Clock.fixed(warsawTime.atZone(WARSAW).toInstant(), WARSAW);
        return new MessageService(
                threadRepository,
                participantRepository,
                messageRepository,
                userRepository,
                threadAccessGuard,
                new WorkingHoursEvaluator(clock),
                notificationPublisher,
                new MessageMapperImpl(),
                clock
        );
    }

    private ChatMessageEntity assignMessageId(org.mockito.invocation.InvocationOnMock invocation) {

        ChatMessageEntity message = invocation.getArgument(0);
        message.setId(MESSAGE_ID);
        message.setCreatedAt(OffsetDateTime.ofInstant(at(2026, 11, 11, 10, 0), WARSAW));
        return message;
    }

    private static ChatThreadEntity thread(UUID id) {

        ChatThreadEntity thread = new ChatThreadEntity();
        thread.setId(id);
        thread.setSubject("Consultation");
        thread.setThreadType(ThreadType.DIRECT);
        return thread;
    }

    private static ChatMessageEntity message(UUID id, UUID senderId, String content) {

        ChatMessageEntity message = new ChatMessageEntity();
        message.setId(id);
        message.setThreadId(THREAD_ID);
        message.setSenderId(senderId);
        message.setContent(content);
        message.setStatus(MessageStatus.DELIVERED);
        message.setCreatedAt(OffsetDateTime.ofInstant(at(2026, 11, 11, 10, 0), WARSAW));
        return message;
    }

    private static UserEntity user(UUID id, Role role, String firstName, String lastName) {

        UserEntity user = new UserEntity();
        user.setId(id);
        user.setRole(role);
        user.setFirstName(firstName);
        user.setLastName(lastName);
        return user;
    }

    private static Instant at(int year, int month, int day, int hour, int minute) {

        return LocalDateTime.of(year, month, day, hour, minute).atZone(WARSAW).toInstant();
    }
}
