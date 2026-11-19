package com.gakkomobile.groupclass;

import com.gakkomobile.ScheduleDataIntegrationTest;
import com.gakkomobile.user.Role;
import com.gakkomobile.user.UserEntity;
import org.junit.jupiter.api.BeforeEach;

import java.util.UUID;

/**
 * Base for group class integration tests. Seeds a small reference data set before every test:
 * one study programme with two groups, one building with two rooms, two subjects and two lecturers.
 */
abstract class GroupClassIntegrationTest extends ScheduleDataIntegrationTest {

    protected UUID groupId;
    protected UUID secondGroupId;
    protected UUID buildingId;
    protected UUID roomId;
    protected UUID secondRoomId;
    protected UUID subjectId;
    protected UUID secondSubjectId;
    protected UUID lecturerId;
    protected UUID secondLecturerId;

    @BeforeEach
    void seedReferenceData() {
        UUID studiesId = insertStudies("Computer Science");
        groupId = insertGroup("WIs I.1", studiesId);
        secondGroupId = insertGroup("WIs I.2", studiesId);

        buildingId = insertBuilding("Main Campus");
        roomId = insertRoom(buildingId, "Aula A");
        secondRoomId = insertRoom(buildingId, "Lab 12");

        subjectId = insertSubject("PRG", "Java Programming", "LECTURE");
        secondSubjectId = insertSubject("ENG", "English B22", "LANGUAGE_COURSE");

        UserEntity lecturer = insertUser(Role.LECTURER, "jan.kowalski@pjatk.edu.pl", "Jan", "Kowalski");
        UserEntity secondLecturer = insertUser(Role.LECTURER, "anna.nowak@pjatk.edu.pl", "Anna", "Nowak");
        lecturerId = lecturer.getId();
        secondLecturerId = secondLecturer.getId();
    }
}
