package com.gakkomobile.messages.service;

import com.gakkomobile.user.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
interface ChatThreadParticipantRepository extends JpaRepository<ChatThreadParticipantEntity, UUID> {

    boolean existsByThreadIdAndUserId(UUID threadId, UUID userId);

    Optional<ChatThreadParticipantEntity> findByThreadIdAndUserId(UUID threadId, UUID userId);

    List<ChatThreadParticipantEntity> findByThreadId(UUID threadId);

    @Query("""
            SELECT user.id
            FROM UserEntity user
            WHERE user.role = :role
              AND user.id IN (
                  SELECT participant.userId
                  FROM ChatThreadParticipantEntity participant
                  WHERE participant.threadId = :threadId
                    AND participant.userId <> :senderId
              )
            """)
    List<UUID> findOtherParticipantsByRole(
            @Param("threadId") UUID threadId,
            @Param("senderId") UUID senderId,
            @Param("role") Role role
    );
}
