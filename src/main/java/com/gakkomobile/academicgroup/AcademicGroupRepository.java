package com.gakkomobile.academicgroup;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
interface AcademicGroupRepository extends JpaRepository<AcademicGroupEntity, UUID> {
    boolean existsByName(String name);

    List<AcademicGroupEntity> findAllByStudies_NameAndAcademicYear(String studiesName, String academicYear);
}
