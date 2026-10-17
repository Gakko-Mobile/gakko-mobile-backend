package com.gakkomobile.notifications;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
interface InAppNotificationRepository extends JpaRepository<InAppNotificationEntity, UUID> {

    Page<InAppNotificationEntity> findAllByUserId(UUID userId, Pageable pageable);

    @Modifying
    @Query("UPDATE InAppNotificationEntity n SET n.isRead = :isRead WHERE n.userId = :userId AND n.isRead != :isRead")
    void updateAllReadStatusByUserId(@Param("userId") UUID userId, @Param("isRead") boolean isRead);

    Optional<InAppNotificationEntity> findByIdAndUserId(UUID id, UUID userId);
}