package com.gakkomobile.notifications.settings;

import static com.gakkomobile.utils.DatasetConstants.uuid;
import static org.assertj.core.api.Assertions.assertThat;

import com.gakkomobile.BaseIntegrationTest;
import com.gakkomobile.auth.dto.UserCreatedEvent;
import com.gakkomobile.notifications.NotificationMode;
import com.gakkomobile.user.Role;
import com.gakkomobile.user.UserEntity;
import com.gakkomobile.user.UserRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.OffsetDateTime;
import java.util.UUID;

class UserCreatedNotificationListenerIntegrationTest extends BaseIntegrationTest {

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private UserNotificationSettingsRepository settingsRepository;

    @Autowired
    private UserRepository userRepository;

    @PersistenceContext
    private EntityManager entityManager;

    private static final UUID NEW_USER_ID = uuid(5);

    @Test
    @DisplayName("Should insert default settings before commit when event is published inside a transaction")
    void handleUserCreated_whenTransactionCommits_shouldInsertDefaultSettings() {

        new TransactionTemplate(transactionManager).executeWithoutResult(_ -> {
            entityManager.persist(newUser());
            entityManager.flush();

            eventPublisher.publishEvent(new UserCreatedEvent(NEW_USER_ID));
        });

        UserNotificationSettingsEntity settings = settingsRepository.findById(NEW_USER_ID).orElseThrow();
        assertThat(settings.getAcademicGrades()).isEqualTo(NotificationMode.PUSH_AND_INAPP);
        assertThat(settings.getAcademicScheduleChanges()).isEqualTo(NotificationMode.PUSH_AND_INAPP);
        assertThat(settings.getAcademicCancelledClasses()).isEqualTo(NotificationMode.PUSH_AND_INAPP);
        assertThat(settings.getFinancialNewCharges()).isEqualTo(NotificationMode.PUSH_AND_INAPP);
        assertThat(settings.getEventsStudent()).isEqualTo(NotificationMode.INAPP_ONLY);
        assertThat(settings.getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("Should not insert settings when the transaction is rolled back")
    void handleUserCreated_whenTransactionRollsBack_shouldNotInsertSettings() {

        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            entityManager.persist(newUser());
            entityManager.flush();

            eventPublisher.publishEvent(new UserCreatedEvent(NEW_USER_ID));
            status.setRollbackOnly();
        });

        assertThat(userRepository.findById(NEW_USER_ID)).isEmpty();
        assertThat(settingsRepository.findById(NEW_USER_ID)).isEmpty();
    }

    @Test
    @DisplayName("Should ignore the event when published outside of a transaction")
    void handleUserCreated_whenNoActiveTransaction_shouldNotInsertSettings() {

        createAndSaveUser(NEW_USER_ID, "new@pjatk.edu.pl");

        eventPublisher.publishEvent(new UserCreatedEvent(NEW_USER_ID));

        assertThat(settingsRepository.findById(NEW_USER_ID)).isEmpty();
    }

    private UserEntity newUser() {

        UserEntity user = new UserEntity();
        user.setId(UserCreatedNotificationListenerIntegrationTest.NEW_USER_ID);
        user.setEmail("new@pjatk.edu.pl");
        user.setPasswordHash("$2a$10$dummyHash");
        user.setFirstName("John");
        user.setLastName("Doe");
        user.setRole(Role.STUDENT);
        user.setPasswordChangedAt(OffsetDateTime.now());
        return user;
    }
}