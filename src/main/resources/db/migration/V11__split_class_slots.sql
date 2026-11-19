CREATE TABLE class_slots
(
    id             UUID PRIMARY KEY,
    group_class_id UUID REFERENCES group_classes (id) ON DELETE CASCADE NOT NULL,
    class_date     DATE                                                 NOT NULL,
    created_at     TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP   NOT NULL,

    UNIQUE (group_class_id, class_date)
);

CREATE INDEX idx_class_slots_class_date ON class_slots (class_date);

ALTER TABLE group_classes
    ADD COLUMN start_time  TIME,
    ADD COLUMN end_time    TIME,
    ADD COLUMN is_one_time BOOLEAN DEFAULT TRUE NOT NULL;

UPDATE group_classes
SET start_time = (start_timestamp AT TIME ZONE 'Europe/Warsaw')::TIME,
    end_time   = (end_timestamp AT TIME ZONE 'Europe/Warsaw')::TIME;

INSERT INTO class_slots (id, group_class_id, class_date)
SELECT gen_random_uuid(), id, (start_timestamp AT TIME ZONE 'Europe/Warsaw')::DATE
FROM group_classes;

ALTER TABLE group_classes
    ALTER COLUMN start_time SET NOT NULL,
    ALTER COLUMN end_time SET NOT NULL;

ALTER TABLE group_classes
    DROP COLUMN start_timestamp,
    DROP COLUMN end_timestamp;

ALTER TABLE group_classes
    ADD CONSTRAINT group_classes_end_time_after_start_time CHECK (end_time > start_time);
