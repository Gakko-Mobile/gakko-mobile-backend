package com.gakkomobile.building;

import com.gakkomobile.building.dto.BuildingIdentifiersResponseDTO;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class BuildingMapperTest {

    private final BuildingMapper mapper = Mappers.getMapper(BuildingMapper.class);

    @Test
    void toDto_shouldMapIdAndName() {
        // given
        BuildingEntity building = new BuildingEntity();
        building.setId(UUID.randomUUID());
        building.setName("Main Campus");

        // when and then
        assertThat(mapper.toDto(building))
                .isEqualTo(new BuildingIdentifiersResponseDTO(building.getId(), "Main Campus"));
    }
}
