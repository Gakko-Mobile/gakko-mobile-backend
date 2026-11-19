package com.gakkomobile.building;

import com.gakkomobile.building.dto.BuildingIdentifiersResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("api/v1/buildings")
@RequiredArgsConstructor
public class BuildingController {
    private final BuildingService buildingService;

    @GetMapping("/identifiers")
    public ResponseEntity<List<BuildingIdentifiersResponseDTO>> getBuildingIdentifiers() {
        return ResponseEntity.ok(buildingService.getAllBuildingIdentifiers());
    }
}
