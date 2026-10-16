package com.gakkomobile.event;

import com.gakkomobile.event.dto.EventResponseDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
public class EventService {

    private final EventRepository eventRepository;

    public List<EventResponseDTO> getEventsByDate(LocalDate date) {
        OffsetDateTime startOfDay = date.atTime(LocalTime.MIN).atOffset(ZoneOffset.UTC);
        OffsetDateTime endOfDay = startOfDay.plusDays(1);

        List<EventEntity> events = eventRepository.findEventsByDate(startOfDay, endOfDay);
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");

        return events.stream().map(event -> new EventResponseDTO(
                event.getId(),
                event.getOrganizator(),
                event.getDescription(),
                event.getBuilding().getName(),
                event.getStartTimestamp().toLocalDate().toString(),
                event.getStartTimestamp().toLocalTime().format(timeFormatter),
                event.getEndTimestamp().toLocalTime().format(timeFormatter)
        )).toList();
    }
}