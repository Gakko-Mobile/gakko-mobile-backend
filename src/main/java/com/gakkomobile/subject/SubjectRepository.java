package com.gakkomobile.subject;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
interface SubjectRepository extends JpaRepository<SubjectEntity, UUID> {
}
