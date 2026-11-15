package com.gakkomobile.messages.service;

import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BufferedMessageSchedulerTest {

    @Mock
    private MessageService messageService;

    @InjectMocks
    private BufferedMessageScheduler scheduler;

    @Test
    void releaseBufferedMessages_shouldReleaseDueMessages() {

        scheduler.releaseBufferedMessages();

        verify(messageService).releaseDueMessages();
    }
}
