package com.gakkomobile.building;

import com.gakkomobile.building.dto.BuildingIdentifiersResponseDTO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BuildingServiceTest {

    @Mock
    private BuildingRepository buildingRepository;

    @Mock
    private BuildingMapper buildingMapper;

    @InjectMocks
    private BuildingService buildingService;

    @Test
    void getAllBuildingIdentifiers_shouldMapBuildingsInRepositoryOrder() {
        // given
        BuildingEntity first = new BuildingEntity();
        BuildingEntity second = new BuildingEntity();
        BuildingIdentifiersResponseDTO firstDto = new BuildingIdentifiersResponseDTO(UUID.randomUUID(), "Building A");
        BuildingIdentifiersResponseDTO secondDto = new BuildingIdentifiersResponseDTO(UUID.randomUUID(), "Building B");

        when(buildingRepository.findAllByOrderByNameAsc()).thenReturn(List.of(first, second));
        when(buildingMapper.toDto(first)).thenReturn(firstDto);
        when(buildingMapper.toDto(second)).thenReturn(secondDto);

        // when and then
        assertThat(buildingService.getAllBuildingIdentifiers()).containsExactly(firstDto, secondDto);
    }

    @Test
    void getAllBuildingIdentifiers_shouldReturnEmptyList_whenThereAreNoBuildings() {
        // given
        when(buildingRepository.findAllByOrderByNameAsc()).thenReturn(List.of());

        // when and then
        assertThat(buildingService.getAllBuildingIdentifiers()).isEmpty();
        verifyNoInteractions(buildingMapper);
    }
}
