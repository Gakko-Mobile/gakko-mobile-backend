package com.gakkomobile;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.gakkomobile.security.jwt.JwtService;
import com.gakkomobile.user.Role;
import com.gakkomobile.user.UserEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * Shared helpers for integration tests that need schedule related data (buildings, rooms, subjects, groups,
 * classes, slots) and accounts with a given role.
 * <p>
 * Reference data is inserted with plain SQL, so the tests stay independent of the entity mappings.
 * Nothing is seeded automatically - tests call only the helpers they need.
 */
public abstract class ScheduleDataIntegrationTest extends BaseIntegrationTest {

    @Autowired
    protected JdbcTemplate jdbcTemplate;

    @Autowired
    protected WebTestClient anonymousClient;

    @Autowired
    private JwtService jwtService;

    /**
     * Authenticates the requests of {@link #authenticatedClient} as a user with the given role
     * ({@code <role>@pjatk.edu.pl}). The account is created on first use.
     */
    protected void loginAs(Role role) {
        String email = role.name().toLowerCase() + "@pjatk.edu.pl";

        List<UUID> existing = jdbcTemplate.queryForList(
                "SELECT id FROM users WHERE email = ?", UUID.class, email);

        if (existing.isEmpty()) {
            insertUser(role, email, "Test", role.name());
        }

        when(jwtService.extractUsername(anyString())).thenReturn(email);
        when(jwtService.isTokenValid(anyString(), any())).thenReturn(true);
    }

    protected UserEntity insertUser(Role role, String email, String firstName, String lastName) {
        UserEntity user = new UserEntity();
        user.setId(UUID.randomUUID());
        user.setRole(role);
        user.setEmail(email);
        user.setPasswordHash("$2a$10$dummyHash");
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setPasswordChangedAt(OffsetDateTime.now());
        return persistAndCommit(user);
    }

    protected UUID insertStudies(String name) {
        UUID id = UUID.randomUUID();
        jdbcTemplate.update("INSERT INTO studies (id, name) VALUES (?, ?)", id, name);
        return id;
    }

    protected UUID insertGroup(String name, UUID studiesId) {
        UUID id = UUID.randomUUID();
        jdbcTemplate.update(
                "INSERT INTO academic_groups (id, name, academic_year, studies_id) VALUES (?, ?, '2026/2027', ?)",
                id, name, studiesId
        );
        return id;
    }

    protected void addToGroup(UUID userId, UUID groupId, boolean itn) {
        jdbcTemplate.update(
                "INSERT INTO user_groups (user_id, group_id, is_itn) VALUES (?, ?, ?)",
                userId, groupId, itn
        );
    }

    protected UUID insertBuilding(String name) {
        UUID id = UUID.randomUUID();
        jdbcTemplate.update("INSERT INTO buildings (id, name) VALUES (?, ?)", id, name);
        return id;
    }

    protected UUID insertRoom(UUID buildingId, String name) {
        UUID id = UUID.randomUUID();
        jdbcTemplate.update("INSERT INTO rooms (id, name, building_id) VALUES (?, ?, ?)", id, name, buildingId);
        return id;
    }

    /**
     * @param type name of a {@code subject_type} enum value, e.g. {@code LECTURE}; it is inlined as an SQL literal
     *             because enum columns do not accept bound varchar parameters.
     */
    protected UUID insertSubject(String code, String name, String type) {
        UUID id = UUID.randomUUID();
        jdbcTemplate.update(
                "INSERT INTO subjects (id, code, name, type, ects_points) VALUES (?, ?, ?, '" + type + "', 5)",
                id, code, name
        );
        return id;
    }

    protected UUID insertClass(
            UUID subjectId,
            UUID roomId,
            LocalTime startTime,
            LocalTime endTime,
            boolean oneTime,
            Collection<UUID> groupIds,
            Collection<UUID> lecturerIds
    ) {
        UUID id = UUID.randomUUID();
        jdbcTemplate.update(
                "INSERT INTO group_classes (id, subject_id, room_id, start_time, end_time, is_one_time) "
                        + "VALUES (?, ?, ?, ?, ?, ?)",
                id, subjectId, roomId, startTime, endTime, oneTime
        );
        groupIds.forEach(groupId -> jdbcTemplate.update(
                "INSERT INTO academic_groups_group_classes (class_id, group_id) VALUES (?, ?)", id, groupId));
        lecturerIds.forEach(lecturerId -> jdbcTemplate.update(
                "INSERT INTO group_class_lecturers (class_id, lecturer_id) VALUES (?, ?)", id, lecturerId));
        return id;
    }

    protected void insertSlot(UUID classId, LocalDate date, String msTeamsCode) {
        jdbcTemplate.update(
                "INSERT INTO class_slots (id, group_class_id, class_date, ms_teams_code) VALUES (?, ?, ?, ?)",
                UUID.randomUUID(), classId, date, msTeamsCode
        );
    }

    protected long count(String table) {
        Long result = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + table, Long.class);
        return result == null ? 0 : result;
    }
}
