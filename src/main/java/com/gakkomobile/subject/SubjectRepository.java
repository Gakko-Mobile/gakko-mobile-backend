package com.gakkomobile.subject;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
interface SubjectRepository extends JpaRepository<SubjectEntity, UUID> {

    @Query("""
        SELECT s FROM SubjectEntity s
        WHERE LOWER(s.name) LIKE LOWER(CONCAT('%', :search, '%'))
           OR LOWER(s.code) LIKE LOWER(CONCAT('%', :search, '%'))
    """)
    List<SubjectEntity> searchByNameOrCode(@Param("search") String search, Pageable pageable);
}
