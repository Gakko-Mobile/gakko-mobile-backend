ALTER TABLE class_slots
    ADD COLUMN ms_teams_code VARCHAR(100);

UPDATE class_slots cs
SET ms_teams_code = gc.ms_teams_code
FROM group_classes gc
WHERE cs.group_class_id = gc.id;

ALTER TABLE group_classes
    DROP COLUMN ms_teams_code;
