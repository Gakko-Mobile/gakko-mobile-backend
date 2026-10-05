package com.gakkomobile.notifications;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
interface UserNotificationRepository extends JpaRepository<UserNotificationEntity, UUID> {

    List<UserNotificationEntity> findAllByUserIdAndIsReadFalseOrderByCreatedAtDesc(String userId);
}