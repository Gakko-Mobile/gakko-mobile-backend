package com.gakkomobile.building;

import com.gakkomobile.building.dto.BuildingIdentifiersResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BuildingService {
    private final BuildingRepository buildingRepository;
    private final BuildingMapper buildingMapper;

    public List<BuildingIdentifiersResponseDTO> getAllBuildingIdentifiers() {
        return buildingRepository.findAllByOrderByNameAsc()
                .stream()
                .map(buildingMapper::toDto)
                .toList();
    }
}
