package com.gakkomobile.notifications;


import static com.gakkomobile.utils.DatasetConstants.uuid;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import com.gakkomobile.exception.ResourceNotFoundException;
import com.gakkomobile.notifications.dto.InAppNotificationReadInfoResponseDTO;
import com.gakkomobile.notifications.dto.InAppNotificationResponseDTO;
import com.gakkomobile.notifications.dto.NotificationDTO;
import com.gakkomobile.notifications.outbox.NotificationOutboxEventFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private InAppNotificationRepository notificationRepository;
    @Mock
    private NotificationOutboxEventFactory notificationOutboxEventFactory;
    @Mock
    private NotificationMapper notificationMapper;
    @Mock
    private NotificationTextResolver notificationTextResolver;

    @InjectMocks
    private NotificationService notificationService;

    @BeforeEach
    void keepStoredTitleWhenTheResolverDoesNotChangeIt() {

        lenient().when(notificationTextResolver.resolve(
                org.mockito.ArgumentMatchers.nullable(String.class),
                org.mockito.ArgumentMatchers.any()
        )).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void putNotificationToOutbox_shouldDelegateToOutboxEventFactory() {

        // given
        NotificationDTO notificationDTO = NotificationDTO.builder().build();

        // when
        notificationService.putNotificationToOutbox(notificationDTO);

        // then
        verify(notificationOutboxEventFactory).create(notificationDTO);
    }

    @Test
    void createInAppNotification_shouldDelegateToRepository() {

        // given
        NotificationDTO notificationDTO = NotificationDTO.builder().build();

        // when
        notificationService.createInAppNotification(notificationDTO);

        // then
        InAppNotificationEntity entity = verify(notificationMapper).dtoToEntity(notificationDTO);
        verify(notificationRepository).save(entity);
    }

    @Test
    void getNotifications_shouldReturnEmptyPage_whenThereAreNoElements() {

        // given
        when(notificationRepository.findAllByUserId(uuid(0), Pageable.unpaged())).thenReturn(Page.empty());

        // when
        Page<InAppNotificationResponseDTO> notifications = notificationService.getNotifications(uuid(0), Pageable.unpaged(), Locale.forLanguageTag("pl"));

        // then
        List<InAppNotificationResponseDTO> content = notifications.getContent();
        assertThat(content).isEqualTo(List.of());
        verify(notificationRepository).findAllByUserId(eq(uuid(0)), any(Pageable.class));

    }

    @Test
    void getNotifications_shouldReturnListOfNotifications_whenThereAreSome() {

        // given
        List<InAppNotificationEntity> existingNotifications = List.of(InAppNotificationEntity.builder().build());
        when(notificationRepository.findAllByUserId(uuid(0), Pageable.unpaged())).thenReturn(new PageImpl<>(existingNotifications));

        // when
        Page<InAppNotificationResponseDTO> notifications = notificationService.getNotifications(uuid(0), Pageable.unpaged(), Locale.forLanguageTag("pl"));

        // then
        List<InAppNotificationResponseDTO> content = notifications.getContent();
        assertThat(content.size()).isEqualTo(1);
        verify(notificationRepository).findAllByUserId(eq(uuid(0)), any(Pageable.class));

    }

    @Test
    void getNotificationById_shouldReturnResult_whenNotificationDoesExist() {

        // given
        InAppNotificationEntity existingNotification = InAppNotificationEntity.builder().build();
        InAppNotificationResponseDTO expectedResponse = InAppNotificationResponseDTO.builder().build();
        when(notificationRepository.findByIdAndUserId(uuid(0), uuid(1))).thenReturn(Optional.of(existingNotification));
        when(notificationMapper.entityToResponseDTO(existingNotification)).thenReturn(expectedResponse);

        // when
        InAppNotificationResponseDTO notification = notificationService.getNotificationById(uuid(0), uuid(1), Locale.forLanguageTag("pl"));

        // then
        assertThat(notification).isNotNull();
        verify(notificationRepository).findByIdAndUserId(uuid(0), uuid(1));
    }

    @Test
    void getNotificationId_shouldThrowException_whenNotificationDoesNotExist(){

        // given
        when(notificationRepository.findByIdAndUserId(uuid(0), uuid(1))).thenReturn(Optional.empty());

        // when & then
        assertThrows(ResourceNotFoundException.class, () -> notificationService.getNotificationById(uuid(0), uuid(1), Locale.forLanguageTag("pl")));
    }

    @Test
    void markNotificationReadStatus_shouldTogglesNotificationReadStatus_whenNotificationExists(){

        // given
        InAppNotificationEntity existingNotification = InAppNotificationEntity.builder().build();
        when(notificationRepository.findByIdAndUserId(uuid(0), uuid(1))).thenReturn(Optional.of(existingNotification));

        // when
        notificationService.markNotificationReadStatus(uuid(0), uuid(1));

        // then
        assertThat(existingNotification.isRead()).isTrue();
        verify(notificationRepository).findByIdAndUserId(uuid(0), uuid(1));
    }

    @Test
    void markNotificationReadStatus_shouldThrowException_whenNotificationDoesNotExist(){

        // given
        when(notificationRepository.findByIdAndUserId(uuid(0), uuid(1))).thenReturn(Optional.empty());

        // when & then
        assertThrows(ResourceNotFoundException.class, () -> notificationService.markNotificationReadStatus(uuid(0), uuid(1)));
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void markNotificationReadStatus_shouldTogglesNotificationReadStatus_whenNotificationExistsAndRequestReadStatusIsTrue(boolean isRead){

        // given
        InAppNotificationEntity existingNotification = InAppNotificationEntity.builder().isRead(isRead).build();
        when(notificationRepository.findByIdAndUserId(uuid(0), uuid(1))).thenReturn(Optional.of(existingNotification));

        // when
        InAppNotificationReadInfoResponseDTO response = notificationService.markNotificationReadStatus(uuid(0), uuid(1));

        // then
        assertThat(response.isRead()).isEqualTo(!isRead);
        verify(notificationRepository).findByIdAndUserId(uuid(0), uuid(1));

    }

    @Test
    void getNotifications_shouldResolveTheTitleFromAcceptLanguage() {

        InAppNotificationEntity entity = InAppNotificationEntity.builder()
                .messageKey(NotificationMessages.MESSAGE_CREATED)
                .build();
        InAppNotificationResponseDTO mapped = InAppNotificationResponseDTO.builder()
                .body("Hello")
                .build();
        when(notificationRepository.findAllByUserId(uuid(0), Pageable.unpaged())).thenReturn(new PageImpl<>(java.util.List.of(entity)));
        when(notificationMapper.entityToResponseDTO(entity)).thenReturn(mapped);
        when(notificationTextResolver.resolve(NotificationMessages.MESSAGE_CREATED, Locale.ENGLISH))
                .thenReturn("New message");

        Page<InAppNotificationResponseDTO> notifications = notificationService.getNotifications(
                uuid(0),
                Pageable.unpaged(),
                Locale.ENGLISH
        );

        assertThat(notifications.getContent().getFirst().title()).isEqualTo("New message");
        assertThat(notifications.getContent().getFirst().body()).isEqualTo("Hello");
    }
}
