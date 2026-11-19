package com.gakkomobile.room;

import com.gakkomobile.exception.ExceptionErrorConstants;
import com.gakkomobile.exception.ResourceNotFoundException;
import com.gakkomobile.room.dto.RoomIdentifiersResponseDTO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RoomServiceTest {

    @Mock
    private RoomRepository roomRepository;

    @Mock
    private RoomMapper roomMapper;

    @InjectMocks
    private RoomService roomService;

    @Test
    void getById_shouldReturnRoom_whenItExists() {
        // given
        UUID id = UUID.randomUUID();
        RoomEntity room = new RoomEntity();
        when(roomRepository.findById(id)).thenReturn(Optional.of(room));

        // when and then
        assertThat(roomService.getById(id)).isSameAs(room);
    }

    @Test
    void getById_shouldThrowResourceNotFound_whenItDoesNotExist() {
        // given
        UUID id = UUID.randomUUID();
        when(roomRepository.findById(id)).thenReturn(Optional.empty());

        // when and then
        assertThatThrownBy(() -> roomService.getById(id))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage(ExceptionErrorConstants.ROOM_NOT_FOUND);
    }

    @Test
    void getRoomIdentifiersByBuildingId_shouldMapRoomsInRepositoryOrder() {
        // given
        UUID buildingId = UUID.randomUUID();
        RoomEntity first = new RoomEntity();
        RoomEntity second = new RoomEntity();
        RoomIdentifiersResponseDTO firstDto = new RoomIdentifiersResponseDTO(UUID.randomUUID(), "Main", "A1");
        RoomIdentifiersResponseDTO secondDto = new RoomIdentifiersResponseDTO(UUID.randomUUID(), "Main", "A2");

        when(roomRepository.findAllByBuildingIdOrderByNameAsc(buildingId)).thenReturn(List.of(first, second));
        when(roomMapper.toDto(first)).thenReturn(firstDto);
        when(roomMapper.toDto(second)).thenReturn(secondDto);

        // when and then
        assertThat(roomService.getRoomIdentifiersByBuildingId(buildingId)).containsExactly(firstDto, secondDto);
    }

    @Test
    void getRoomIdentifiersByBuildingId_shouldReturnEmptyList_whenBuildingHasNoRooms() {
        // given
        UUID buildingId = UUID.randomUUID();
        when(roomRepository.findAllByBuildingIdOrderByNameAsc(buildingId)).thenReturn(List.of());

        // when and then
        assertThat(roomService.getRoomIdentifiersByBuildingId(buildingId)).isEmpty();
        verifyNoInteractions(roomMapper);
    }
}
