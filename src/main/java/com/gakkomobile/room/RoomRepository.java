package com.gakkomobile.room;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
interface RoomRepository extends JpaRepository<RoomEntity, UUID> {
    List<RoomEntity> findAllByBuildingIdOrderByNameAsc(UUID buildingId);
}
