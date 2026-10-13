package com.gakkomobile.notifications.settings;


import com.gakkomobile.auth.dto.UserCreatedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class UserCreatedNotificationListener {

    private final UserNotificationSettingsRepository settingsRepository;

    @TransactionalEventListener(phase = TransactionPhase.BEFORE_COMMIT)
    public void handleUserCreated(UserCreatedEvent event) {

        settingsRepository.insertDefaultSettingsNative(event.userId());
    }
}
