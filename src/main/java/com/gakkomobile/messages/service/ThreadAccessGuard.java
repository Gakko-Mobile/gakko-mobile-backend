package com.gakkomobile.messages.service;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ThreadAccessGuard {

    private final ChatThreadParticipantRepository participantRepository;

    public void requireParticipant(UUID threadId, UUID userId) {

        if (!participantRepository.existsByThreadIdAndUserId(threadId, userId)) {
            throw new AccessDeniedException("User is not a participant of this thread");
        }
    }

    ChatThreadParticipantEntity requireMembership(UUID threadId, UUID userId) {

        return participantRepository.findByThreadIdAndUserId(threadId, userId)
                .orElseThrow(() -> new AccessDeniedException("User is not a participant of this thread"));
    }
}
