package com.gakkomobile.groupclass;

import com.gakkomobile.groupclass.entity.GroupClass;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface GroupClassRepository extends JpaRepository<GroupClass, UUID> {
    @Query("""
        SELECT DISTINCT gc FROM GroupClass gc
        JOIN gc.groups ag
        WHERE ag IN (
            SELECT gm.group FROM GroupMembership gm WHERE gm.user.email = :email
        )
        AND gc.startTimestamp >= :startOfDay
        AND gc.startTimestamp < :endOfDay
        ORDER BY gc.startTimestamp ASC
    """)
    List<GroupClass> findClassesByUserEmailAndDate(
            @Param("email") String email,
            @Param("startOfDay") OffsetDateTime startOfDay,
            @Param("endOfDay") OffsetDateTime endOfDay
    );

    @Query("""
                SELECT gc FROM GroupClass gc
                JOIN gc.groups g
                WHERE g.name = :groupName
                AND gc.startTimestamp >= :startOfDay
                AND gc.startTimestamp < :endOfDay
                ORDER BY gc.startTimestamp ASC
            """)
    List<GroupClass> findClassesByGroupNameAndDate(
            @Param("groupName") String groupName,
            @Param("startOfDay") OffsetDateTime startOfDay,
            @Param("endOfDay") OffsetDateTime endOfDay
    );
}
