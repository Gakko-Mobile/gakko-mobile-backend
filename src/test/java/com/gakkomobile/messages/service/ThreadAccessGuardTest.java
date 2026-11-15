package com.gakkomobile.messages.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.gakkomobile.utils.DatasetConstants;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.util.Optional;
import java.util.UUID;

@ExtendWith(MockitoExtension.class)
class ThreadAccessGuardTest {

    private static final UUID USER_ID = DatasetConstants.uuid(1);
    private static final UUID THREAD_ID = DatasetConstants.uuid(2);

    @Mock
    private ChatThreadParticipantRepository participantRepository;

    @InjectMocks
    private ThreadAccessGuard threadAccessGuard;

    @Test
    void requireParticipant_shouldAllowAMember() {

        when(participantRepository.existsByThreadIdAndUserId(THREAD_ID, USER_ID)).thenReturn(true);

        assertThatCode(() -> threadAccessGuard.requireParticipant(THREAD_ID, USER_ID))
                .doesNotThrowAnyException();
    }

    @Test
    void requireParticipant_shouldRejectAnOutsider() {

        when(participantRepository.existsByThreadIdAndUserId(THREAD_ID, USER_ID)).thenReturn(false);

        assertThatThrownBy(() -> threadAccessGuard.requireParticipant(THREAD_ID, USER_ID))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("User is not a participant of this thread");
    }

    @Test
    void requireMembership_shouldReturnTheParticipantRow() {

        ChatThreadParticipantEntity membership = new ChatThreadParticipantEntity();
        membership.setThreadId(THREAD_ID);
        membership.setUserId(USER_ID);
        when(participantRepository.findByThreadIdAndUserId(THREAD_ID, USER_ID)).thenReturn(Optional.of(membership));

        assertThat(threadAccessGuard.requireMembership(THREAD_ID, USER_ID)).isSameAs(membership);
    }

    @Test
    void requireMembership_shouldRejectAnOutsider() {

        when(participantRepository.findByThreadIdAndUserId(THREAD_ID, USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> threadAccessGuard.requireMembership(THREAD_ID, USER_ID))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("User is not a participant of this thread");
    }
}
