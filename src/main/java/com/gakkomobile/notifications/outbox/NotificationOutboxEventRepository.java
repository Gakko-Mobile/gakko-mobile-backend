package com.gakkomobile.notifications.outbox;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
interface NotificationOutboxEventRepository extends JpaRepository<NotificationOutboxEventEntity, UUID> {

    List<NotificationOutboxEventEntity> findTop50ByProcessedFalseOrderByCreatedAtAsc();
}
