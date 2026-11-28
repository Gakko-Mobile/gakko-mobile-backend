package com.gakkomobile.notifications;

import com.gakkomobile.exception.ResourceNotFoundException;
import com.gakkomobile.notifications.dto.InAppNotificationReadInfoResponseDTO;
import com.gakkomobile.notifications.dto.InAppNotificationResponseDTO;
import com.gakkomobile.notifications.dto.NotificationDTO;
import com.gakkomobile.notifications.outbox.NotificationOutboxEventFactory;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.Objects;
import java.util.UUID;


@Service
@RequiredArgsConstructor
class NotificationService {

    private final InAppNotificationRepository notificationRepository;
    private final NotificationOutboxEventFactory notificationOutboxEventFactory;
    private final NotificationMapper notificationMapper;
    private final NotificationTextResolver notificationTextResolver;

    public void putNotificationToOutbox(NotificationDTO notificationDTO) {

        notificationOutboxEventFactory.create(notificationDTO);
    }

    @Transactional
    public void createInAppNotification(NotificationDTO notificationDTO) {

        notificationRepository.save(notificationMapper.dtoToEntity(notificationDTO));
    }

    public Page<InAppNotificationResponseDTO> getNotifications(UUID userId, Pageable pageable, Locale locale) {

        return notificationRepository.findAllByUserId(userId, pageable)
                .map(entity -> localize(notificationMapper.entityToResponseDTO(entity), entity, locale));
    }

    public InAppNotificationResponseDTO getNotificationById(UUID notificationId, UUID userId, Locale locale) {

        return notificationRepository.findByIdAndUserId(notificationId, userId)
                .map(entity -> localize(notificationMapper.entityToResponseDTO(entity), entity, locale))
                .orElseThrow(() -> new ResourceNotFoundException(String.format("Notification with id [%s] not found", notificationId)));
    }

    @Transactional
    public InAppNotificationReadInfoResponseDTO markNotificationReadStatus(UUID notificationId, UUID userId) {

        InAppNotificationEntity inAppNotification = notificationRepository.findByIdAndUserId(notificationId, userId)
                .orElseThrow(() -> new ResourceNotFoundException(String.format("Notification with id [%s] not found", notificationId)));

        boolean currentReadStatus = toggleNotificationReadStatus(inAppNotification);
        return new InAppNotificationReadInfoResponseDTO(inAppNotification.getId(), currentReadStatus);
    }

    private boolean toggleNotificationReadStatus(InAppNotificationEntity inAppNotification){

        inAppNotification.setRead(!inAppNotification.isRead());
        return inAppNotification.isRead();
    }

    @Transactional
    public void markAllNotificationsReadStatus(UUID userId, boolean requestReadStatus) {

        notificationRepository.updateAllReadStatusByUserId(userId, requestReadStatus);
    }

    private InAppNotificationResponseDTO localize(InAppNotificationResponseDTO response, InAppNotificationEntity entity,
            Locale locale) {

        if (Objects.isNull(response)) {
            return null;
        }
        String title = notificationTextResolver.resolve(entity.getMessageKey(), locale);
        return new InAppNotificationResponseDTO(
                response.id(),
                response.userId(),
                title,
                response.body(),
                response.category(),
                response.isRead(),
                response.createdAt()
        );
    }
}
