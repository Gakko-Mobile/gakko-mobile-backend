package com.gakkomobile.building;

import com.gakkomobile.ScheduleDataIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

class BuildingIdentifiersIntegrationTest extends ScheduleDataIntegrationTest {

    private static final String URI = "/api/v1/buildings/identifiers";

    @Test
    @DisplayName("Should return all buildings sorted by name with their ids")
    void shouldReturnAllBuildingsSortedByName() {
        // given
        insertBuilding("Building C");
        UUID buildingA = insertBuilding("Building A");
        insertBuilding("Building B");

        // when and then
        authenticatedClient.get()
                .uri(URI)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.length()").isEqualTo(3)
                .jsonPath("$[0].id").isEqualTo(buildingA.toString())
                .jsonPath("$[0].name").isEqualTo("Building A")
                .jsonPath("$[1].name").isEqualTo("Building B")
                .jsonPath("$[2].name").isEqualTo("Building C")
                .jsonPath("$[0].rooms").doesNotExist();
    }

    @Test
    @DisplayName("Should return an empty list when there are no buildings")
    void shouldReturnEmptyList_whenThereAreNoBuildings() {
        authenticatedClient.get()
                .uri(URI)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.length()").isEqualTo(0);
    }

    @Test
    @DisplayName("Should reject an unauthenticated request")
    void shouldReturnForbidden_whenUnauthenticated() {
        anonymousClient.get()
                .uri(URI)
                .exchange()
                .expectStatus().isForbidden();
    }
}
