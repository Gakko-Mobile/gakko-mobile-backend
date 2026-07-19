CREATE TYPE user_role AS ENUM ('STUDENT', 'EMPLOYEE', 'CANDIDATE', 'GUEST');
CREATE TYPE schedule_status AS ENUM ('SCHEDULED', 'CANCELLED', 'COMPLETED');
CREATE TYPE notification_mode AS ENUM ('PUSH_AND_INAPP', 'INAPP_ONLY', 'OFF');
CREATE TYPE subject_type AS ENUM ('LECTURE', 'LABORATORY', 'EXERCISE', 'PROJECT', 'SEMINAR');
CREATE TYPE notification_target_type AS ENUM ('TARGET_USER', 'TARGET_GROUP', 'TARGET_ALL_STUDENTS', 'TARGET_ALL_EMPLOYEES');


CREATE TABLE users
(
    id                  UUID PRIMARY KEY,   -- Time-ordered UUIDv7 generated application-side
    role                user_role                                          NOT NULL,
    email               VARCHAR(255) UNIQUE                                NOT NULL,
    password_hash       VARCHAR(255)                                       NOT NULL,
    first_name          VARCHAR(100)                                       NOT NULL,
    last_name           VARCHAR(100)                                       NOT NULL,
    index_number        VARCHAR(20) UNIQUE, -- Nullable for employees, candidates, and guests
    pesel               CHAR(11) UNIQUE,
    biometric_token     TEXT,               -- Auth token for mobile biometrics (FaceID/TouchID)
    password_changed_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    created_at          TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE INDEX idx_users_email ON users (email);


CREATE TABLE academic_groups
(
    id            UUID PRIMARY KEY,
    name          VARCHAR(50) UNIQUE                                 NOT NULL,
    academic_year VARCHAR(10)                                        NOT NULL,
    semester      INT                                                NOT NULL,
    created_at    TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL
);


CREATE TABLE subjects
(
    id          UUID PRIMARY KEY,
    code        VARCHAR(20) UNIQUE                                 NOT NULL,
    name        VARCHAR(150)                                       NOT NULL,
    ects_points INT                                                NOT NULL CHECK (ects_points >= 0),
    created_at  TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL
);


CREATE TABLE user_groups
(
    user_id  UUID REFERENCES users (id) ON DELETE CASCADE,
    group_id UUID REFERENCES academic_groups (id) ON DELETE CASCADE,
    PRIMARY KEY (user_id, group_id)
);

CREATE INDEX idx_user_groups_group ON user_groups (group_id);


CREATE TABLE subject_lecturers
(
    subject_id  UUID REFERENCES subjects (id) ON DELETE CASCADE,
    lecturer_id UUID REFERENCES users (id) ON DELETE CASCADE,
    PRIMARY KEY (subject_id, lecturer_id)
);



CREATE TABLE schedules
(
    id          UUID PRIMARY KEY,
    group_id    UUID REFERENCES academic_groups (id) ON DELETE CASCADE NOT NULL,
    subject_id  UUID REFERENCES subjects (id) ON DELETE CASCADE        NOT NULL,
    lecturer_id UUID REFERENCES users (id) ON DELETE CASCADE           NOT NULL,
    type        subject_type                                           NOT NULL,
    classroom   VARCHAR(50)                                            NOT NULL,
    start_time  TIMESTAMP WITH TIME ZONE                               NOT NULL,
    end_time    TIMESTAMP WITH TIME ZONE                               NOT NULL,
    status      schedule_status          DEFAULT 'SCHEDULED'           NOT NULL,
    updated_at  TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP     NOT NULL
);

CREATE INDEX idx_schedules_group_time ON schedules (group_id, start_time, end_time);


CREATE TABLE user_notification_settings
(
    user_id                    UUID PRIMARY KEY REFERENCES users (id) ON DELETE CASCADE,
    academic_grades            notification_mode        DEFAULT 'PUSH_AND_INAPP'  NOT NULL,
    academic_schedule_changes  notification_mode        DEFAULT 'PUSH_AND_INAPP'  NOT NULL,
    academic_cancelled_classes notification_mode        DEFAULT 'PUSH_AND_INAPP'  NOT NULL,
    financial_new_charges      notification_mode        DEFAULT 'PUSH_AND_INAPP'  NOT NULL,
    events_student             notification_mode        DEFAULT 'INAPP_ONLY'      NOT NULL,
    updated_at                 TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL
);