package com.gakkomobile.room;

import com.gakkomobile.room.dto.RoomIdentifiersResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("api/v1/rooms")
@RequiredArgsConstructor
public class RoomController {
    private final RoomService roomService;

    @GetMapping("/identifiers")
    public ResponseEntity<List<RoomIdentifiersResponseDTO>> getRoomIdentifiers(
            @RequestParam(name = "building-id") UUID buildingId
    ) {
        return ResponseEntity.ok(roomService.getRoomIdentifiersByBuildingId(buildingId));
    }
}
