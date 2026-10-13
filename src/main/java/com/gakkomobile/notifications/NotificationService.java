package com.gakkomobile.notifications;

import com.gakkomobile.exception.ResourceNotFoundException;
import com.gakkomobile.notifications.dto.InAppNotificationDTO;
import com.gakkomobile.notifications.dto.InAppNotificationReadInfoResponseDTO;
import com.gakkomobile.notifications.dto.InAppNotificationResponseDTO;
import com.gakkomobile.notifications.dto.NotificationDTO;
import com.gakkomobile.notifications.outbox.NotificationOutboxEventFactory;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.UUID;


@Service
@RequiredArgsConstructor
class NotificationService {

    private final InAppNotificationRepository notificationRepository;
    private final NotificationOutboxEventFactory notificationOutboxEventFactory;
    private final NotificationMapper notificationMapper;

    @Transactional
    public void putNotificationToOutbox(NotificationDTO notificationDTO) {

        notificationOutboxEventFactory.create(notificationDTO);
    }

    @Transactional
    public void createInAppNotification(NotificationDTO notificationDTO) {

        notificationRepository.save(notificationMapper.dtoToEntity(notificationDTO));
    }

    public Page<InAppNotificationResponseDTO> getNotifications(UUID userId, Pageable pageable) {

        return notificationRepository.findAllByUserId(userId, pageable)
                .map(notificationMapper::entityToResponseDTO);
    }

    public InAppNotificationResponseDTO getNotificationById(UUID notificationId, UUID userId) {

        return notificationRepository.findByIdAndUserId(notificationId, userId)
                .map(notificationMapper::entityToResponseDTO)
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
}
