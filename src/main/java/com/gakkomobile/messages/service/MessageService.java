package com.gakkomobile.messages.service;

import static com.gakkomobile.exception.ExceptionErrorConstants.CHAT_THREAD_NOT_FOUND;
import static com.gakkomobile.exception.ExceptionErrorConstants.USER_NOT_FOUND;
import com.gakkomobile.exception.ExceptionErrorConstants;
import com.gakkomobile.exception.ResourceNotFoundException;
import com.gakkomobile.messages.domain.MessageStatus;
import com.gakkomobile.messages.dto.ChatMessageResponseDTO;
import com.gakkomobile.messages.dto.ChatThreadSummaryResponseDTO;
import com.gakkomobile.messages.dto.CreateThreadRequestDTO;
import com.gakkomobile.messages.dto.SendMessagePayloadDTO;
import com.gakkomobile.user.Role;
import com.gakkomobile.user.UserEntity;
import com.gakkomobile.user.UserRepository;
import com.gakkomobile.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MessageService {

    private final ChatThreadRepository threadRepository;
    private final ChatThreadParticipantRepository participantRepository;
    private final ChatMessageRepository messageRepository;
    private final UserService userService;
    private final ThreadAccessGuard threadAccessGuard;
    private final WorkingHoursEvaluator workingHoursEvaluator;
    private final MessageNotificationPublisher notificationPublisher;
    private final MessageMapper messageMapper;
    private final Clock clock;

    @Transactional
    public ChatThreadSummaryResponseDTO createThread(UUID creatorId, CreateThreadRequestDTO request) {

        userService.requireUser(creatorId);
        Set<UUID> participantIds = new LinkedHashSet<>();
        participantIds.add(creatorId);
        participantIds.addAll(request.participantIds());
        for (UUID participantId : participantIds) {
            userService.requireUser(participantId);
        }

        ChatThreadEntity thread = new ChatThreadEntity();
        thread.setSubject(request.subject().trim());
        thread.setThreadType(request.threadType());
        ChatThreadEntity savedThread = threadRepository.save(thread);

        for (UUID participantId : participantIds) {
            ChatThreadParticipantEntity participant = new ChatThreadParticipantEntity();
            participant.setThreadId(savedThread.getId());
            participant.setUserId(participantId);
            participantRepository.save(participant);
        }

        return messageMapper.toNewThreadSummary(savedThread);
    }

    @Transactional(readOnly = true)
    public List<ChatThreadSummaryResponseDTO> listThreads(UUID userId) {

        return messageMapper.toSummaries(threadRepository.findSummariesForUser(userId));
    }

    @Transactional(readOnly = true)
    public Page<ChatMessageResponseDTO> listMessages(UUID userId, UUID threadId, Pageable pageable) {

        threadAccessGuard.requireParticipant(threadId, userId);
        Page<ChatMessageEntity> messages = messageRepository.findVisible(threadId, userId, pageable);
        Map<UUID, UserEntity> senders = loadSenders(messages.getContent());
        return messages.map(message -> messageMapper.toResponse(message, displayName(senders.get(message.getSenderId()))));
    }

    @Transactional
    public ChatMessageResponseDTO sendMessage(UUID senderId, UUID threadId, SendMessagePayloadDTO payload) {

        threadAccessGuard.requireParticipant(threadId, senderId);
        UserEntity sender = userService.requireUser(senderId);
        ChatThreadEntity thread = threadRepository.findById(threadId)
                .orElseThrow(() -> new ResourceNotFoundException(CHAT_THREAD_NOT_FOUND.formatted(threadId)));

        boolean lecturerRecipient = !participantRepository
                .findOtherParticipantsByRole(threadId, senderId, Role.LECTURER)
                .isEmpty();
        boolean buffer = workingHoursEvaluator.shouldBuffer(sender.getRole(), lecturerRecipient, payload.isUrgent());

        ChatMessageEntity message = new ChatMessageEntity();
        message.setThreadId(threadId);
        message.setSenderId(senderId);
        message.setContent(payload.content().trim());
        message.setUrgent(payload.isUrgent());
        if (buffer) {
            message.setBuffered(true);
            message.setStatus(MessageStatus.BUFFERED);
            message.setScheduledDeliveryAt(workingHoursEvaluator.nextBusinessOpening());
        } else {
            message.setBuffered(false);
            message.setStatus(MessageStatus.DELIVERED);
        }

        ChatMessageEntity savedMessage = messageRepository.saveAndFlush(message);
        thread.setUpdatedAt(OffsetDateTime.now(clock));
        threadRepository.save(thread);

        if (!buffer) {
            notificationPublisher.publishDelivered(savedMessage);
        }
        return messageMapper.toResponse(savedMessage, displayName(sender));
    }

    @Transactional
    public void markThreadAsRead(UUID userId, UUID threadId) {

        ChatThreadParticipantEntity membership = threadAccessGuard.requireMembership(threadId, userId);
        membership.setLastReadAt(OffsetDateTime.now(clock));
    }

    @Transactional
    public void releaseDueMessages() {

        List<ChatMessageEntity> dueMessages = messageRepository.findDueBuffered(OffsetDateTime.now(clock));
        for (ChatMessageEntity message : dueMessages) {
            message.setBuffered(false);
            message.setStatus(MessageStatus.DELIVERED);
            ChatMessageEntity savedMessage = messageRepository.saveAndFlush(message);
            notificationPublisher.publishDelivered(savedMessage);
        }
    }

    private Map<UUID, UserEntity> loadSenders(List<ChatMessageEntity> messages) {

        Set<UUID> senderIds = messages.stream()
                .map(ChatMessageEntity::getSenderId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (senderIds.isEmpty()) {
            return Map.of();
        }
        return userService.getUsersDictionaryByIds(senderIds);
    }

    private String displayName(UserEntity user) {

        if (Objects.isNull(user)) {
            return null;
        }
        return String.format("%s %s", user.getFirstName(), user.getLastName());
    }
}
