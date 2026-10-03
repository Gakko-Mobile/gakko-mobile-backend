package com.gakkomobile.academicgroup;

import com.gakkomobile.academicgroup.entity.AcademicGroup;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AcademicGroupRepository extends JpaRepository<AcademicGroup, UUID> {
    boolean existsByName(String name);

//    @Query("""
//                SELECT ag FROM AcademicGroup ag
//                WHERE LOWER(ag.name) LIKE LOWER(CONCAT('%', :name, '%'))
//            """)
//    List<AcademicGroup> searchByName(@Param("name") String name, Pageable pageable);
}
