package com.gakkomobile.groupclass.dto;

import java.util.List;
import java.util.Set;

public record GroupClassWithSlotsResponseDTO(
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
        List<ClassSlotDTO> slots
) {
}
