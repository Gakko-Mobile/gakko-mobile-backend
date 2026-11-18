package com.gakkomobile.groupclass;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
interface ClassSlotRepository extends JpaRepository<ClassSlotEntity, UUID> {
    boolean existsByGroupClassId(UUID groupClassId);

    boolean existsByGroupClassIdAndClassDate(UUID groupClassId, LocalDate classDate);

    @Query("""
        SELECT cs FROM ClassSlotEntity cs
        JOIN cs.groupClass gc
        WHERE cs.classDate = :date
        AND EXISTS (
            SELECT 1 FROM GroupMembershipEntity gm
            WHERE gm.user.email = :email
            AND gm.group MEMBER OF gc.groups
        )
        ORDER BY gc.startTime ASC
    """)
    List<ClassSlotEntity> findSlotsByUserEmailAndDate(
            @Param("email") String email,
            @Param("date") LocalDate date
    );

    @Query("""
        SELECT cs FROM ClassSlotEntity cs
        JOIN cs.groupClass gc
        JOIN gc.groups g
        WHERE g.id = :groupId
        AND cs.classDate = :date
        ORDER BY gc.startTime ASC
    """)
    List<ClassSlotEntity> findSlotsByGroupIdAndDate(
            @Param("groupId") UUID groupId,
            @Param("date") LocalDate date
    );
}
