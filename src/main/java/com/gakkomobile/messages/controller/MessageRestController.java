package com.gakkomobile.messages.controller;

import com.gakkomobile.messages.dto.ChatMessageResponseDTO;
import com.gakkomobile.messages.dto.ChatThreadSummaryResponseDTO;
import com.gakkomobile.messages.dto.CreateThreadRequestDTO;
import com.gakkomobile.messages.dto.SendMessagePayloadDTO;
import com.gakkomobile.messages.service.MessageService;
import com.gakkomobile.user.UserEntity;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/messages")
@RequiredArgsConstructor
public class MessageRestController {

    private final MessageService messageService;

    @PostMapping("/threads")
    public ResponseEntity<ChatThreadSummaryResponseDTO> createThread(
            @AuthenticationPrincipal UserEntity currentUser,
            @Valid @RequestBody CreateThreadRequestDTO request
    ) {

        return ResponseEntity.status(HttpStatus.CREATED).body(messageService.createThread(currentUser.getId(), request));
    }

    @GetMapping("/threads")
    public ResponseEntity<List<ChatThreadSummaryResponseDTO>> listThreads(
            @AuthenticationPrincipal UserEntity currentUser
    ) {

        return ResponseEntity.ok(messageService.listThreads(currentUser.getId()));
    }

    @GetMapping("/threads/{threadId}/messages")
    public ResponseEntity<Page<ChatMessageResponseDTO>> listMessages(
            @AuthenticationPrincipal UserEntity currentUser,
            @PathVariable UUID threadId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {

        return ResponseEntity.ok(messageService.listMessages(currentUser.getId(), threadId, pageable));
    }

    @PostMapping("/threads/{threadId}/messages")
    public ResponseEntity<ChatMessageResponseDTO> sendMessage(
            @AuthenticationPrincipal UserEntity currentUser,
            @PathVariable UUID threadId,
            @Valid @RequestBody SendMessagePayloadDTO payload
    ) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(messageService.sendMessage(currentUser.getId(), threadId, payload));
    }

    @PostMapping("/threads/{threadId}/read")
    public ResponseEntity<Void> markAsRead(
            @AuthenticationPrincipal UserEntity currentUser,
            @PathVariable UUID threadId
    ) {

        messageService.markThreadAsRead(currentUser.getId(), threadId);
        return ResponseEntity.noContent().build();
    }
}
