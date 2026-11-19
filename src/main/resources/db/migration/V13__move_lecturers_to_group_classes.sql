CREATE TABLE group_class_lecturers
(
    class_id    UUID NOT NULL REFERENCES group_classes (id) ON DELETE CASCADE,
    lecturer_id UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    PRIMARY KEY (class_id, lecturer_id)
);

CREATE INDEX idx_group_class_lecturers_lecturer ON group_class_lecturers (lecturer_id);

-- Every lecturer who belonged to a group becomes a lecturer of all classes of that group.
INSERT INTO group_class_lecturers (class_id, lecturer_id)
SELECT DISTINCT agc.class_id, ug.user_id
FROM academic_groups_group_classes agc
         JOIN user_groups ug ON ug.group_id = agc.group_id
         JOIN users u ON u.id = ug.user_id
WHERE u.role = 'LECTURER';

-- Group memberships are for students only.
DELETE
FROM user_groups ug
    USING users u
WHERE u.id = ug.user_id
  AND u.role <> 'STUDENT';
