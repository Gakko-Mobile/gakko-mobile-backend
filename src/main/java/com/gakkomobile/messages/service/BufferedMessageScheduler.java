package com.gakkomobile.messages.service;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class BufferedMessageScheduler {

    private final MessageService messageService;

    @Scheduled(cron = "0 */5 * * * *")
    void releaseBufferedMessages() {

        messageService.releaseDueMessages();
    }
}
