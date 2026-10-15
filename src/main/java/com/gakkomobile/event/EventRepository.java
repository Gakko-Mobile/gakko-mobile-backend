package com.gakkomobile.event;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface EventRepository extends JpaRepository<Event, UUID> {

    @Query("""
        SELECT e FROM Event e
        JOIN FETCH e.building b
        WHERE e.startTimestamp >= :startOfDay
          AND e.startTimestamp < :endOfDay
        ORDER BY e.startTimestamp ASC
    """)
    List<Event> findEventsByDate(
            @Param("startOfDay") OffsetDateTime startOfDay,
            @Param("endOfDay") OffsetDateTime endOfDay
    );
}