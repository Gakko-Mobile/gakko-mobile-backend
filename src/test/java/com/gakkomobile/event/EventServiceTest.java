package com.gakkomobile.event;

import com.gakkomobile.building.Building;
import com.gakkomobile.event.dto.EventResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EventServiceTest {

    @Mock
    private EventRepository eventRepository;

    @InjectMocks
    private EventService eventService;

    @Test
    void getEventsByDate_ShouldReturnMappedResponses_WhenEventsFound() {
        LocalDate targetDate = LocalDate.of(2026, 10, 3);
        OffsetDateTime startOfDay = targetDate.atTime(LocalTime.MIN).atOffset(ZoneOffset.UTC);
        OffsetDateTime endOfDay = startOfDay.plusDays(1);

        Building building = new Building();
        building.setId(UUID.randomUUID());
        building.setName("Main Campus Building");

        Event event1 = new Event();
        event1.setId(UUID.randomUUID());
        event1.setOrganizator("Career Services");
        event1.setDescription("Tech Industry Resume Workshop");
        event1.setBuilding(building);
        event1.setStartTimestamp(OffsetDateTime.of(2026, 10, 3, 16, 0, 0, 0, ZoneOffset.UTC));
        event1.setEndTimestamp(OffsetDateTime.of(2026, 10, 3, 18, 0, 0, 0, ZoneOffset.UTC));

        Event event2 = new Event();
        event2.setId(UUID.randomUUID());
        event2.setOrganizator("Student Board");
        event2.setDescription("Welcome Semester Integration Party");
        event2.setBuilding(building);
        event2.setStartTimestamp(OffsetDateTime.of(2026, 10, 3, 19, 30, 0, 0, ZoneOffset.UTC));
        event2.setEndTimestamp(OffsetDateTime.of(2026, 10, 3, 23, 45, 0, 0, ZoneOffset.UTC));

        when(eventRepository.findEventsByDate(startOfDay, endOfDay))
                .thenReturn(List.of(event1, event2));

        List<EventResponse> result = eventService.getEventsByDate(targetDate);

        assertThat(result).hasSize(2);

        EventResponse response1 = result.get(0);
        assertThat(response1.id()).isEqualTo(event1.getId());
        assertThat(response1.organizator()).isEqualTo("Career Services");
        assertThat(response1.description()).isEqualTo("Tech Industry Resume Workshop");
        assertThat(response1.buildingName()).isEqualTo("Main Campus Building");
        assertThat(response1.eventDate()).isEqualTo("2026-10-03");
        assertThat(response1.startTime()).isEqualTo("16:00");
        assertThat(response1.endTime()).isEqualTo("18:00");

        EventResponse response2 = result.get(1);
        assertThat(response2.id()).isEqualTo(event2.getId());
        assertThat(response2.organizator()).isEqualTo("Student Board");
        assertThat(response2.startTime()).isEqualTo("19:30");
        assertThat(response2.endTime()).isEqualTo("23:45");

        verify(eventRepository).findEventsByDate(startOfDay, endOfDay);
    }

    @Test
    void getEventsByDate_ShouldReturnEmptyList_WhenNoEventsFound() {
        LocalDate targetDate = LocalDate.of(2026, 10, 4);
        OffsetDateTime startOfDay = targetDate.atTime(LocalTime.MIN).atOffset(ZoneOffset.UTC);
        OffsetDateTime endOfDay = startOfDay.plusDays(1);

        when(eventRepository.findEventsByDate(startOfDay, endOfDay))
                .thenReturn(Collections.emptyList());

        List<EventResponse> result = eventService.getEventsByDate(targetDate);

        assertThat(result).isEmpty();
        verify(eventRepository).findEventsByDate(startOfDay, endOfDay);
    }
}