package com.gakkomobile.groupclass.dto;

import java.util.Set;

public record GroupClassResponseDTO(
        int regularStudentsNum,
        int itnStudentsNum,
        String subjectName,
        String subjectCode,
        String classType,
        Set<String> groups,
        Set<String> lecturers,
        String building,
        String room,
        String classDate,
        String startTime,
        String endTime,
        int durationInMins,
        String msTeamsCode
) {

}
