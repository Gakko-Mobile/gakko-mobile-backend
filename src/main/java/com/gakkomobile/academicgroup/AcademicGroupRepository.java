package com.gakkomobile.academicgroup;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AcademicGroupRepository extends JpaRepository<AcademicGroupEntity, UUID> {
    boolean existsByName(String name);

    List<AcademicGroupEntity> findAllByStudies_NameAndAcademicYear(String studiesName, String academicYear);

//    @Query("""
//                SELECT ag FROM AcademicGroup ag
//                WHERE LOWER(ag.name) LIKE LOWER(CONCAT('%', :name, '%'))
//            """)
//    List<AcademicGroup> searchByName(@Param("name") String name, Pageable pageable);
}
