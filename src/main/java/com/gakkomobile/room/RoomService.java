package com.gakkomobile.room;

import com.gakkomobile.exception.ExceptionErrorConstants;
import com.gakkomobile.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RoomService {
    private final RoomRepository roomRepository;

    public RoomEntity getById(UUID id) {
        return roomRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(ExceptionErrorConstants.ROOM_NOT_FOUND));
    }
}
