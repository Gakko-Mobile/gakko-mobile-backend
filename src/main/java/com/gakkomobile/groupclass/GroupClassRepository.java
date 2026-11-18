package com.gakkomobile.groupclass;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
interface GroupClassRepository extends JpaRepository<GroupClassEntity, UUID> {
}
