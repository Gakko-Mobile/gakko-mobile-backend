package com.gakkomobile.room;

import com.gakkomobile.exception.ExceptionErrorConstants;
import com.gakkomobile.exception.ResourceNotFoundException;
import com.gakkomobile.room.dto.RoomIdentifiersResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RoomService {
    private final RoomRepository roomRepository;
    private final RoomMapper roomMapper;

    public RoomEntity getById(UUID id) {
        return roomRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(ExceptionErrorConstants.ROOM_NOT_FOUND));
    }

    @Transactional(readOnly = true)
    public List<RoomIdentifiersResponseDTO> getRoomIdentifiersByBuildingId(UUID buildingId) {
        return roomRepository.findAllByBuildingIdOrderByNameAsc(buildingId)
                .stream()
                .map(roomMapper::toDto)
                .toList();
    }
}
