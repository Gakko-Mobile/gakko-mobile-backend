package com.gakkomobile.room;

import com.gakkomobile.building.BuildingEntity;
import com.gakkomobile.room.dto.RoomIdentifiersResponseDTO;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class RoomMapperTest {

    private final RoomMapper mapper = Mappers.getMapper(RoomMapper.class);

    @Test
    void toDto_shouldMapIdNameAndBuildingName() {
        // given
        BuildingEntity building = new BuildingEntity();
        building.setName("Main Campus");

        RoomEntity room = new RoomEntity();
        room.setId(UUID.randomUUID());
        room.setName("Aula A");
        room.setBuilding(building);

        // when and then
        assertThat(mapper.toDto(room))
                .isEqualTo(new RoomIdentifiersResponseDTO(room.getId(), "Main Campus", "Aula A"));
    }
}
