package com.gakkomobile.messages.service;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
interface ChatThreadRepository extends JpaRepository<ChatThreadEntity, UUID> {

    @Query(value = """
            SELECT
                thread.id AS "threadId",
                thread.subject AS "subject",
                thread.thread_type AS "threadType",
                last_message.created_at AS "lastMessageAt",
                last_message.content AS "lastMessageContent",
                (
                    SELECT COUNT(*)
                    FROM chat_messages unread_message
                    WHERE unread_message.thread_id = thread.id
                      AND unread_message.is_buffered = FALSE
                      AND unread_message.sender_id IS DISTINCT FROM :userId
                      AND (
                          participant.last_read_at IS NULL
                          OR unread_message.created_at > participant.last_read_at
                      )
                ) AS "unreadCount"
            FROM chat_threads thread
            JOIN chat_thread_participants participant
              ON participant.thread_id = thread.id
             AND participant.user_id = :userId
            LEFT JOIN LATERAL (
                SELECT message.content, message.created_at
                FROM chat_messages message
                WHERE message.thread_id = thread.id
                  AND (message.is_buffered = FALSE OR message.sender_id = :userId)
                ORDER BY message.created_at DESC
                LIMIT 1
            ) last_message ON TRUE
            ORDER BY last_message.created_at DESC NULLS LAST, thread.created_at DESC
            """, nativeQuery = true)
    List<ChatThreadSummaryView> findSummariesForUser(@Param("userId") UUID userId);
}
