package com.gakkomobile.subject;

import com.gakkomobile.ScheduleDataIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

class SubjectIdentifiersIntegrationTest extends ScheduleDataIntegrationTest {

    private static final String URI = "/api/v1/subjects/identifiers";

    private UUID algorithmsId;

    @BeforeEach
    void seedSubjects() {
        insertSubject("MAD", "Mathematical Analysis", "LECTURE");
        algorithmsId = insertSubject("ALG", "Algorithms", "LECTURE");
        insertSubject("ENG", "English B22", "LANGUAGE_COURSE");
        insertSubject("JAV", "Java Programming", "TUTORIAL");
        insertSubject("BAZ", "Database Basics", "LECTURE");
        insertSubject("PRJ", "Diploma Project", "DIPLOMA_PROJECT");
        insertSubject("NET", "Computer Networks", "LECTURE");
    }

    @Test
    @DisplayName("Should return the first five subjects sorted by name for an empty search")
    void shouldReturnFirstPageSortedByName_forEmptySearch() {
        // when and then
        authenticatedClient.get()
                .uri(uri -> uri.path(URI).queryParam("name-or-code-search", "").build())
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.length()").isEqualTo(5)
                .jsonPath("$[0].name").isEqualTo("Algorithms")
                .jsonPath("$[1].name").isEqualTo("Computer Networks")
                .jsonPath("$[2].name").isEqualTo("Database Basics")
                .jsonPath("$[3].name").isEqualTo("Diploma Project")
                .jsonPath("$[4].name").isEqualTo("English B22");
    }

    @Test
    @DisplayName("Should return only id, code and name")
    void shouldReturnIdCodeAndName() {
        // when and then
        authenticatedClient.get()
                .uri(uri -> uri.path(URI).queryParam("name-or-code-search", "algo").build())
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.length()").isEqualTo(1)
                .jsonPath("$[0].id").isEqualTo(algorithmsId.toString())
                .jsonPath("$[0].code").isEqualTo("ALG")
                .jsonPath("$[0].name").isEqualTo("Algorithms")
                .jsonPath("$[0].type").doesNotExist()
                .jsonPath("$[0].ectsPoints").doesNotExist();
    }

    @Test
    @DisplayName("Should find subjects by a case-insensitive part of the name")
    void shouldSearchByName() {
        // when and then
        authenticatedClient.get()
                .uri(uri -> uri.path(URI).queryParam("name-or-code-search", "JAVA").build())
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.length()").isEqualTo(1)
                .jsonPath("$[0].code").isEqualTo("JAV");
    }

    @Test
    @DisplayName("Should find subjects by a case-insensitive part of the code")
    void shouldSearchByCode() {
        // when and then
        authenticatedClient.get()
                // "baz" is a part of the code BAZ only - the subject name does not contain it
                .uri(uri -> uri.path(URI).queryParam("name-or-code-search", "baz").build())
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.length()").isEqualTo(1)
                .jsonPath("$[0].code").isEqualTo("BAZ")
                .jsonPath("$[0].name").isEqualTo("Database Basics");
    }

    @Test
    @DisplayName("Should return subjects matching either the name or the code, sorted by name")
    void shouldSearchByNameOrCode() {
        // when and then
        authenticatedClient.get()
                // "PRJ" matches by code, "Java Programming" and "Diploma Project" by name
                .uri(uri -> uri.path(URI).queryParam("name-or-code-search", "pr").build())
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.length()").isEqualTo(2)
                .jsonPath("$[0].name").isEqualTo("Diploma Project")
                .jsonPath("$[1].name").isEqualTo("Java Programming");
    }

    @Test
    @DisplayName("Should return the requested page")
    void shouldReturnRequestedPage() {
        // when and then
        authenticatedClient.get()
                .uri(uri -> uri.path(URI)
                        .queryParam("name-or-code-search", "")
                        .queryParam("page", 1)
                        .queryParam("limit", 5)
                        .build())
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.length()").isEqualTo(2)
                .jsonPath("$[0].name").isEqualTo("Java Programming")
                .jsonPath("$[1].name").isEqualTo("Mathematical Analysis");
    }

    @Test
    @DisplayName("Should respect a custom limit")
    void shouldRespectLimit() {
        // when and then
        authenticatedClient.get()
                .uri(uri -> uri.path(URI)
                        .queryParam("name-or-code-search", "")
                        .queryParam("limit", 2)
                        .build())
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.length()").isEqualTo(2)
                .jsonPath("$[0].name").isEqualTo("Algorithms")
                .jsonPath("$[1].name").isEqualTo("Computer Networks");
    }

    @Test
    @DisplayName("Should return an empty list when nothing matches")
    void shouldReturnEmptyList_whenNothingMatches() {
        // when and then
        authenticatedClient.get()
                .uri(uri -> uri.path(URI).queryParam("name-or-code-search", "zzz").build())
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.length()").isEqualTo(0);
    }

    @Test
    @DisplayName("Should return 400 when the search parameter is missing")
    void shouldReturnBadRequest_whenSearchIsMissing() {
        // when and then
        authenticatedClient.get()
                .uri(URI)
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    @DisplayName("Should return 400 when the page is not a number")
    void shouldReturnBadRequest_whenPageIsNotANumber() {
        // when and then
        authenticatedClient.get()
                .uri(uri -> uri.path(URI)
                        .queryParam("name-or-code-search", "")
                        .queryParam("page", "first")
                        .build())
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    @DisplayName("Should reject an unauthenticated request")
    void shouldReturnForbidden_whenUnauthenticated() {
        // when and then
        anonymousClient.get()
                .uri(uri -> uri.path(URI).queryParam("name-or-code-search", "").build())
                .exchange()
                .expectStatus().isForbidden();
    }
}
