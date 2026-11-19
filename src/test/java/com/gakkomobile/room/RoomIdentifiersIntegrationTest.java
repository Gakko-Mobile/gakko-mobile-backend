package com.gakkomobile.room;

import com.gakkomobile.ScheduleDataIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

class RoomIdentifiersIntegrationTest extends ScheduleDataIntegrationTest {

    private static final String URI = "/api/v1/rooms/identifiers";

    private UUID mainBuildingId;
    private UUID annexBuildingId;
    private UUID roomA2Id;

    @BeforeEach
    void seedRooms() {
        mainBuildingId = insertBuilding("Main Campus");
        annexBuildingId = insertBuilding("Annex");

        insertRoom(mainBuildingId, "Lab 12");
        roomA2Id = insertRoom(mainBuildingId, "Aula A");
        insertRoom(mainBuildingId, "Room 7");
        insertRoom(annexBuildingId, "Annex 1");
    }

    @Test
    @DisplayName("Should return only rooms of the given building sorted by name")
    void shouldReturnRoomsOfTheBuildingSortedByName() {
        // when and then
        authenticatedClient.get()
                .uri(uri -> uri.path(URI).queryParam("building-id", mainBuildingId).build())
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.length()").isEqualTo(3)
                .jsonPath("$[0].id").isEqualTo(roomA2Id.toString())
                .jsonPath("$[0].name").isEqualTo("Aula A")
                .jsonPath("$[0].buildingName").isEqualTo("Main Campus")
                .jsonPath("$[1].name").isEqualTo("Lab 12")
                .jsonPath("$[2].name").isEqualTo("Room 7");
    }

    @Test
    @DisplayName("Should not mix rooms of different buildings")
    void shouldNotReturnRoomsOfOtherBuildings() {
        // when and then
        authenticatedClient.get()
                .uri(uri -> uri.path(URI).queryParam("building-id", annexBuildingId).build())
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.length()").isEqualTo(1)
                .jsonPath("$[0].name").isEqualTo("Annex 1")
                .jsonPath("$[0].buildingName").isEqualTo("Annex");
    }

    @Test
    @DisplayName("Should return an empty list for a building without rooms")
    void shouldReturnEmptyList_whenBuildingHasNoRooms() {
        // given
        UUID emptyBuildingId = insertBuilding("Empty Building");

        // when and then
        authenticatedClient.get()
                .uri(uri -> uri.path(URI).queryParam("building-id", emptyBuildingId).build())
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.length()").isEqualTo(0);
    }

    @Test
    @DisplayName("Should return an empty list for an unknown building")
    void shouldReturnEmptyList_whenBuildingDoesNotExist() {
        // when and then
        authenticatedClient.get()
                .uri(uri -> uri.path(URI).queryParam("building-id", UUID.randomUUID()).build())
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.length()").isEqualTo(0);
    }

    @Test
    @DisplayName("Should return 400 when the building id is missing")
    void shouldReturnBadRequest_whenBuildingIdIsMissing() {
        // when and then
        authenticatedClient.get()
                .uri(URI)
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    @DisplayName("Should return 400 when the building id is not a valid UUID")
    void shouldReturnBadRequest_whenBuildingIdIsMalformed() {
        // when and then
        authenticatedClient.get()
                .uri(uri -> uri.path(URI).queryParam("building-id", "not-a-uuid").build())
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    @DisplayName("Should reject an unauthenticated request")
    void shouldReturnForbidden_whenUnauthenticated() {
        // when and then
        anonymousClient.get()
                .uri(uri -> uri.path(URI).queryParam("building-id", mainBuildingId).build())
                .exchange()
                .expectStatus().isForbidden();
    }
}
