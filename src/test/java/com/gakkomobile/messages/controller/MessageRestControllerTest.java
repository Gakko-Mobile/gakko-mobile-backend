package com.gakkomobile.messages.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.gakkomobile.messages.domain.MessageStatus;
import com.gakkomobile.messages.domain.ThreadType;
import com.gakkomobile.messages.dto.ChatMessageResponseDTO;
import com.gakkomobile.messages.dto.ChatThreadSummaryResponseDTO;
import com.gakkomobile.messages.dto.CreateThreadRequestDTO;
import com.gakkomobile.messages.dto.SendMessagePayloadDTO;
import com.gakkomobile.messages.service.MessageService;
import com.gakkomobile.user.UserEntity;
import com.gakkomobile.utils.DatasetConstants;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.UUID;

@ExtendWith(MockitoExtension.class)
class MessageRestControllerTest {

    private static final UUID USER_ID = DatasetConstants.uuid(1);
    private static final UUID THREAD_ID = DatasetConstants.uuid(2);

    @Mock
    private MessageService messageService;

    @InjectMocks
    private MessageRestController controller;

    @Test
    void createThread_shouldReturnCreated() {

        UserEntity user = user();
        CreateThreadRequestDTO request = new CreateThreadRequestDTO("Consultation", ThreadType.DIRECT, List.of());
        ChatThreadSummaryResponseDTO summary = new ChatThreadSummaryResponseDTO(
                THREAD_ID, "Consultation", ThreadType.DIRECT, null, null, 0
        );
        when(messageService.createThread(USER_ID, request)).thenReturn(summary);

        ResponseEntity<ChatThreadSummaryResponseDTO> response = controller.createThread(user, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isEqualTo(summary);
    }

    @Test
    void listThreads_shouldReturnOk() {

        UserEntity user = user();
        List<ChatThreadSummaryResponseDTO> summaries = List.of();
        when(messageService.listThreads(USER_ID)).thenReturn(summaries);

        ResponseEntity<List<ChatThreadSummaryResponseDTO>> response = controller.listThreads(user);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isSameAs(summaries);
    }

    @Test
    void sendMessage_shouldReturnCreated() {

        UserEntity user = user();
        SendMessagePayloadDTO payload = new SendMessagePayloadDTO("Hello", false);
        ChatMessageResponseDTO message = new ChatMessageResponseDTO(
                UUID.randomUUID(), THREAD_ID, USER_ID, "Ada Nowak", "Hello",
                false, false, null, MessageStatus.DELIVERED, null
        );
        when(messageService.sendMessage(USER_ID, THREAD_ID, payload)).thenReturn(message);

        ResponseEntity<ChatMessageResponseDTO> response = controller.sendMessage(user, THREAD_ID, payload);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isEqualTo(message);
    }

    @Test
    void listMessages_shouldReturnThePage() {

        UserEntity user = user();
        PageRequest pageable = PageRequest.of(0, 20);
        PageImpl<ChatMessageResponseDTO> page = new PageImpl<>(List.of());
        when(messageService.listMessages(USER_ID, THREAD_ID, pageable)).thenReturn(page);

        ResponseEntity<?> response = controller.listMessages(user, THREAD_ID, pageable);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isSameAs(page);
    }

    @Test
    void markAsRead_shouldReturnNoContent() {

        UserEntity user = user();

        ResponseEntity<Void> response = controller.markAsRead(user, THREAD_ID);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(messageService).markThreadAsRead(USER_ID, THREAD_ID);
    }

    private static UserEntity user() {

        UserEntity user = new UserEntity();
        user.setId(USER_ID);
        return user;
    }
}
