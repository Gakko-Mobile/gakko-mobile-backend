package com.gakkomobile.messages.service;

import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Repository
interface ChatMessageRepository extends JpaRepository<ChatMessageEntity, UUID> {

    @Query("""
            SELECT message
            FROM ChatMessageEntity message
            WHERE message.threadId = :threadId
              AND (message.buffered = FALSE OR message.senderId = :userId)
            """)
    Page<ChatMessageEntity> findVisible(
            @Param("threadId") UUID threadId,
            @Param("userId") UUID userId,
            Pageable pageable
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(@QueryHint(name = "jakarta.persistence.lock.timeout", value = "-2"))
    @Query("""
            SELECT message
            FROM ChatMessageEntity message
            WHERE message.buffered = TRUE
              AND message.scheduledDeliveryAt <= :now
            ORDER BY message.scheduledDeliveryAt
            """)
    List<ChatMessageEntity> findDueBuffered(@Param("now") OffsetDateTime now);
}
