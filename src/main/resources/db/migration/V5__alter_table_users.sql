ALTER TABLE users ALTER pesel type varchar(11);

ALTER TABLE users
    ALTER COLUMN role TYPE VARCHAR(50) USING role::VARCHAR;

ALTER TABLE subjects
    ALTER COLUMN type TYPE VARCHAR(50) USING type::VARCHAR;

ALTER TABLE applications
    ALTER COLUMN status SET DEFAULT 'SUBMITTED',
    ALTER COLUMN status TYPE VARCHAR(50) USING status::VARCHAR;

ALTER TABLE payments
    ALTER COLUMN type TYPE VARCHAR(50) USING type::VARCHAR,
    ALTER COLUMN status SET DEFAULT 'PENDING',
    ALTER COLUMN status TYPE VARCHAR(50) USING status::VARCHAR;

ALTER TABLE recruitment_applications
    ALTER COLUMN status SET DEFAULT 'DRAFT',
    ALTER COLUMN status TYPE VARCHAR(50) USING status::VARCHAR;

ALTER TABLE user_notification_settings
    ALTER COLUMN academic_grades SET DEFAULT 'PUSH_AND_INAPP',
    ALTER COLUMN academic_grades TYPE VARCHAR(50) USING academic_grades::VARCHAR,
    ALTER COLUMN academic_schedule_changes SET DEFAULT 'PUSH_AND_INAPP',
    ALTER COLUMN academic_schedule_changes TYPE VARCHAR(50) USING academic_schedule_changes::VARCHAR,
    ALTER COLUMN academic_cancelled_classes SET DEFAULT 'PUSH_AND_INAPP',
    ALTER COLUMN academic_cancelled_classes TYPE VARCHAR(50) USING academic_cancelled_classes::VARCHAR,
    ALTER COLUMN financial_new_charges SET DEFAULT 'PUSH_AND_INAPP',
    ALTER COLUMN financial_new_charges TYPE VARCHAR(50) USING financial_new_charges::VARCHAR,
    ALTER COLUMN events_student SET DEFAULT 'INAPP_ONLY',
    ALTER COLUMN events_student TYPE VARCHAR(50) USING events_student::VARCHAR;

ALTER TABLE outbox_events
    ALTER COLUMN target_type TYPE VARCHAR(50) USING target_type::VARCHAR;


DROP TYPE IF EXISTS user_role;
DROP TYPE IF EXISTS notification_mode;
DROP TYPE IF EXISTS subject_type;
DROP TYPE IF EXISTS notification_target_type;
DROP TYPE IF EXISTS application_status;
DROP TYPE IF EXISTS payment_status;
DROP TYPE IF EXISTS payment_type;
DROP TYPE IF EXISTS recruitment_status;