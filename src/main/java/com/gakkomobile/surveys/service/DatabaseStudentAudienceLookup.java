package com.gakkomobile.surveys.service;

import com.gakkomobile.surveys.domain.AudienceTargetType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Component
class DatabaseStudentAudienceLookup {

    private final JdbcTemplate jdbcTemplate;

    DatabaseStudentAudienceLookup(JdbcTemplate jdbcTemplate) {

        this.jdbcTemplate = jdbcTemplate;
    }

    Set<String> findGroupTokens(UUID userIdentifier) {

        List<GroupTokenRow> rows = jdbcTemplate.query("""
                SELECT academic_group.id AS group_identifier, academic_group.name AS group_name
                FROM user_groups membership
                JOIN academic_groups academic_group ON academic_group.id = membership.group_id
                WHERE membership.user_id = ?
                """, (resultSet, rowNumber) -> new GroupTokenRow(
                resultSet.getObject("group_identifier", UUID.class),
                resultSet.getString("group_name")
        ), userIdentifier);

        Set<String> tokens = new LinkedHashSet<>();
        for (GroupTokenRow row : rows) {
            if (!Objects.isNull(row.groupIdentifier())) {
                tokens.add(row.groupIdentifier().toString());
            }
            if (!Objects.isNull(row.groupName()) && !row.groupName().isBlank()) {
                tokens.add(row.groupName());
            }
        }
        return tokens;
    }

    Set<String> findSubjectTokens(UUID userIdentifier) {

        List<SubjectTokenRow> rows = jdbcTemplate.query("""
                SELECT subject.id AS subject_identifier, subject.code AS subject_code, subject.name AS subject_name
                FROM user_groups membership
                JOIN academic_groups_group_classes link ON link.group_id = membership.group_id
                JOIN group_classes group_class ON group_class.id = link.class_id
                JOIN subjects subject ON subject.id = group_class.subject_id
                WHERE membership.user_id = ?
                """, (resultSet, rowNumber) -> new SubjectTokenRow(
                resultSet.getObject("subject_identifier", UUID.class),
                resultSet.getString("subject_code"),
                resultSet.getString("subject_name")
        ), userIdentifier);

        Set<String> tokens = new LinkedHashSet<>();
        for (SubjectTokenRow row : rows) {
            if (!Objects.isNull(row.subjectIdentifier())) {
                tokens.add(row.subjectIdentifier().toString());
            }
            addTextToken(tokens, row.subjectCode());
            addTextToken(tokens, row.subjectName());
        }
        return tokens;
    }

    Long countAudience(AudienceTargetType targetType, List<String> targetIds) {

        if (targetType == AudienceTargetType.VIA_LINK) {
            return null;
        }
        if (targetType == AudienceTargetType.ALL) {
            return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM users", Long.class);
        }
        if (targetType == AudienceTargetType.GROUPS) {
            return countGroupAudience(separateTargets(targetIds));
        }
        return countCourseAudience(separateTargets(targetIds));
    }

    List<UUID> findUsersWithoutCompletion(
            UUID surveyIdentifier,
            AudienceTargetType targetType,
            List<String> targetIds
    ) {

        if (targetType == AudienceTargetType.VIA_LINK) {
            return List.of();
        }
        if (targetType == AudienceTargetType.ALL) {
            return jdbcTemplate.query("""
                    SELECT users.id
                    FROM users
                    WHERE NOT EXISTS (
                        SELECT 1 FROM survey_completions completion
                        WHERE completion.survey_id = ? AND completion.user_id = users.id
                    )
                    """, (resultSet, rowNumber) -> resultSet.getObject(1, UUID.class), surveyIdentifier);
        }
        if (targetType == AudienceTargetType.GROUPS) {
            return findGroupRecipients(surveyIdentifier, separateTargets(targetIds));
        }
        return findCourseRecipients(surveyIdentifier, separateTargets(targetIds));
    }

    List<String> findGroupIdentifiersForCourseTargets(List<String> targetIds) {

        SeparatedTargets targets = separateTargets(targetIds);
        if (targets.identifiers().isEmpty() && targets.names().isEmpty()) {
            return List.of();
        }

        StringBuilder query = new StringBuilder("""
                SELECT DISTINCT academic_group.id
                FROM academic_groups academic_group
                JOIN academic_groups_group_classes link ON link.group_id = academic_group.id
                JOIN group_classes group_class ON group_class.id = link.class_id
                JOIN subjects subject ON subject.id = group_class.subject_id
                WHERE 
                """);
        List<Object> parameters = new ArrayList<>();
        query.append(subjectMatchClause(targets, parameters));

        return jdbcTemplate.query(
                query.toString(),
                (resultSet, rowNumber) -> resultSet.getObject(1, UUID.class),
                parameters.toArray()
        ).stream().map(UUID::toString).toList();
    }

    private Long countGroupAudience(SeparatedTargets targets) {

        if (targets.identifiers().isEmpty() && targets.names().isEmpty()) {
            return 0L;
        }
        StringBuilder query = new StringBuilder("""
                SELECT COUNT(DISTINCT membership.user_id)
                FROM user_groups membership
                JOIN academic_groups academic_group ON academic_group.id = membership.group_id
                WHERE 
                """);
        List<Object> parameters = new ArrayList<>();
        query.append(groupMatchClause(targets, parameters));
        Long count = jdbcTemplate.queryForObject(query.toString(), Long.class, parameters.toArray());
        if (Objects.isNull(count)) {
            return 0L;
        }
        return count;
    }

    private Long countCourseAudience(SeparatedTargets targets) {

        if (targets.identifiers().isEmpty() && targets.names().isEmpty()) {
            return 0L;
        }
        StringBuilder query = new StringBuilder("""
                SELECT COUNT(DISTINCT membership.user_id)
                FROM user_groups membership
                JOIN academic_groups_group_classes link ON link.group_id = membership.group_id
                JOIN group_classes group_class ON group_class.id = link.class_id
                JOIN subjects subject ON subject.id = group_class.subject_id
                WHERE 
                """);
        List<Object> parameters = new ArrayList<>();
        query.append(subjectMatchClause(targets, parameters));
        Long count = jdbcTemplate.queryForObject(query.toString(), Long.class, parameters.toArray());
        if (Objects.isNull(count)) {
            return 0L;
        }
        return count;
    }

    private List<UUID> findGroupRecipients(UUID surveyIdentifier, SeparatedTargets targets) {

        if (targets.identifiers().isEmpty() && targets.names().isEmpty()) {
            return List.of();
        }
        StringBuilder query = new StringBuilder("""
                SELECT DISTINCT membership.user_id
                FROM user_groups membership
                JOIN academic_groups academic_group ON academic_group.id = membership.group_id
                WHERE 
                """);
        List<Object> parameters = new ArrayList<>();
        query.append(groupMatchClause(targets, parameters));
        query.append("""
                 AND NOT EXISTS (
                    SELECT 1 FROM survey_completions completion
                    WHERE completion.survey_id = ? AND completion.user_id = membership.user_id
                )
                """);
        parameters.add(surveyIdentifier);
        return jdbcTemplate.query(
                query.toString(),
                (resultSet, rowNumber) -> resultSet.getObject(1, UUID.class),
                parameters.toArray()
        );
    }

    private List<UUID> findCourseRecipients(UUID surveyIdentifier, SeparatedTargets targets) {

        if (targets.identifiers().isEmpty() && targets.names().isEmpty()) {
            return List.of();
        }
        StringBuilder query = new StringBuilder("""
                SELECT DISTINCT membership.user_id
                FROM user_groups membership
                JOIN academic_groups_group_classes link ON link.group_id = membership.group_id
                JOIN group_classes group_class ON group_class.id = link.class_id
                JOIN subjects subject ON subject.id = group_class.subject_id
                WHERE 
                """);
        List<Object> parameters = new ArrayList<>();
        query.append(subjectMatchClause(targets, parameters));
        query.append("""
                 AND NOT EXISTS (
                    SELECT 1 FROM survey_completions completion
                    WHERE completion.survey_id = ? AND completion.user_id = membership.user_id
                )
                """);
        parameters.add(surveyIdentifier);
        return jdbcTemplate.query(
                query.toString(),
                (resultSet, rowNumber) -> resultSet.getObject(1, UUID.class),
                parameters.toArray()
        );
    }

    private String groupMatchClause(SeparatedTargets targets, List<Object> parameters) {

        List<String> clauses = new ArrayList<>();
        if (!targets.identifiers().isEmpty()) {
            clauses.add(inClause("academic_group.id", targets.identifiers().size()));
            parameters.addAll(targets.identifiers());
        }
        if (!targets.names().isEmpty()) {
            clauses.add(inClause("academic_group.name", targets.names().size()));
            parameters.addAll(targets.names());
        }
        return "(" + String.join(" OR ", clauses) + ")";
    }

    private String subjectMatchClause(SeparatedTargets targets, List<Object> parameters) {

        List<String> clauses = new ArrayList<>();
        if (!targets.identifiers().isEmpty()) {
            clauses.add(inClause("subject.id", targets.identifiers().size()));
            parameters.addAll(targets.identifiers());
        }
        if (!targets.names().isEmpty()) {
            clauses.add(inClause("subject.code", targets.names().size()));
            parameters.addAll(targets.names());
            clauses.add(inClause("subject.name", targets.names().size()));
            parameters.addAll(targets.names());
        }
        return "(" + String.join(" OR ", clauses) + ")";
    }

    private String inClause(String columnName, int size) {

        StringBuilder placeholders = new StringBuilder();
        for (int index = 0; index < size; index++) {
            if (index > 0) {
                placeholders.append(", ");
            }
            placeholders.append("?");
        }
        return columnName + " IN (" + placeholders + ")";
    }

    private SeparatedTargets separateTargets(List<String> targetIds) {

        List<UUID> identifiers = new ArrayList<>();
        List<String> names = new ArrayList<>();
        if (Objects.isNull(targetIds)) {
            return new SeparatedTargets(identifiers, names);
        }
        for (String targetId : targetIds) {
            if (Objects.isNull(targetId) || targetId.isBlank()) {
                continue;
            }
            String trimmedTarget = targetId.trim();
            try {
                identifiers.add(UUID.fromString(trimmedTarget));
            } catch (IllegalArgumentException exception) {
                names.add(trimmedTarget);
            }
        }
        return new SeparatedTargets(identifiers, names);
    }

    private void addTextToken(Collection<String> tokens, String value) {

        if (!Objects.isNull(value) && !value.isBlank()) {
            tokens.add(value);
        }
    }

    private record GroupTokenRow(UUID groupIdentifier, String groupName) {
    }

    private record SubjectTokenRow(UUID subjectIdentifier, String subjectCode, String subjectName) {
    }

    private record SeparatedTargets(List<UUID> identifiers, List<String> names) {
    }
}
