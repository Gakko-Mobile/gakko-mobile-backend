CREATE TYPE user_role AS ENUM ('STUDENT', 'EMPLOYEE', 'CANDIDATE', 'GUEST', 'ADMIN');
CREATE TYPE notification_mode AS ENUM ('PUSH_AND_INAPP', 'INAPP_ONLY', 'OFF');
CREATE TYPE subject_type AS ENUM ('LECTURE', 'LABORATORY', 'EXERCISE', 'PROJECT', 'SEMINAR');
CREATE TYPE notification_target_type AS ENUM ('TARGET_USER', 'TARGET_GROUP', 'TARGET_ALL_STUDENTS', 'TARGET_ALL_EMPLOYEES');
CREATE TYPE application_status AS ENUM ('SUBMITTED', 'IN_REVIEW', 'APPROVED', 'REJECTED', 'ADDITIONAL_INFO_REQUIRED');
CREATE TYPE payment_status AS ENUM ('PENDING', 'PAID', 'FAILED', 'REFUNDED');
CREATE TYPE payment_type AS ENUM ('TUITION_FEE', 'STUDENT_CARD', 'REPEATED_COURSE', 'OTHER');
CREATE TYPE recruitment_status AS ENUM ('DRAFT', 'SUBMITTED', 'DOCUMENTS_VERIFIED', 'ACCEPTED', 'REJECTED');


CREATE TABLE users
(
    id                  UUID PRIMARY KEY, -- Time-ordered UUIDv7 generated application-side
    role                user_role                                          NOT NULL,
    email               VARCHAR(255) UNIQUE                                NOT NULL,
    password_hash       VARCHAR(255)                                       NOT NULL,
    first_name          VARCHAR(100)                                       NOT NULL,
    last_name           VARCHAR(100)                                       NOT NULL,
    index_number        VARCHAR(20) UNIQUE,
    pesel               CHAR(11) UNIQUE,
    biometric_token     TEXT,
    mfa_enabled         BOOLEAN                  DEFAULT FALSE             NOT NULL,
    mfa_secret          VARCHAR(100),
    password_changed_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    created_at          TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE INDEX idx_users_email ON users (email);
CREATE INDEX idx_users_role ON users (role);

CREATE TABLE blacklisted_tokens
(
    token VARCHAR(100) PRIMARY KEY,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE academic_groups
(
    id            UUID PRIMARY KEY,
    name          VARCHAR(50) UNIQUE                                 NOT NULL, -- e.g., 'WIs I.1', 'WIs I.2'
    academic_year VARCHAR(10)                                        NOT NULL,
    semester      INT                                                NOT NULL,
    created_at    TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE TABLE user_groups
(
    user_id  UUID REFERENCES users (id) ON DELETE CASCADE,
    group_id UUID REFERENCES academic_groups (id) ON DELETE CASCADE,
    PRIMARY KEY (user_id, group_id)
);

CREATE INDEX idx_user_groups_group ON user_groups (group_id);

CREATE TABLE subjects
(
    id          UUID PRIMARY KEY,
    code        VARCHAR(20) UNIQUE                                 NOT NULL, -- e.g., 'MAS', 'ICK'
    name        VARCHAR(150)                                       NOT NULL,
    type        subject_type                                       NOT NULL,
    ects_points INT                                                NOT NULL CHECK (ects_points >= 0),
    created_at  TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE TABLE grades
(
    id          UUID PRIMARY KEY,
    student_id  UUID REFERENCES users (id) ON DELETE CASCADE       NOT NULL,
    subject_id  UUID REFERENCES subjects (id) ON DELETE CASCADE    NOT NULL,
    lecturer_id UUID REFERENCES users (id) ON DELETE CASCADE       NOT NULL,
    grade_value NUMERIC(3, 1)                                      NOT NULL,
    weight      NUMERIC(3, 2)            DEFAULT 1.00              NOT NULL,
    term        INT                      DEFAULT 1                 NOT NULL,
    comment     TEXT,
    created_at  TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE INDEX idx_grades_student ON grades (student_id);

CREATE TABLE applications
(
    id               UUID PRIMARY KEY,
    student_id       UUID REFERENCES users (id) ON DELETE CASCADE       NOT NULL,
    type             VARCHAR(100)                                       NOT NULL,
    title            VARCHAR(200)                                       NOT NULL,
    justification    TEXT                                               NOT NULL,
    status           application_status       DEFAULT 'SUBMITTED'       NOT NULL,
    reviewer_id      UUID REFERENCES users (id),
    reviewer_comment TEXT,
    updated_at       TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    created_at       TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE TABLE application_attachments
(
    id             UUID PRIMARY KEY,
    application_id UUID REFERENCES applications (id) ON DELETE CASCADE NOT NULL,
    file_name      VARCHAR(255)                                        NOT NULL,
    file_type      VARCHAR(100)                                        NOT NULL,
    file_url       TEXT                                                NOT NULL,
    uploaded_at    TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP  NOT NULL
);

CREATE TABLE payments
(
    id                    UUID PRIMARY KEY,
    student_id            UUID REFERENCES users (id) ON DELETE CASCADE       NOT NULL,
    title                 VARCHAR(200)                                       NOT NULL,
    amount                NUMERIC(10, 2)                                     NOT NULL,
    currency              CHAR(3)                  DEFAULT 'PLN'             NOT NULL,
    due_date              DATE                                               NOT NULL,
    type                  payment_type                                       NOT NULL,
    status                payment_status           DEFAULT 'PENDING'         NOT NULL,
    transaction_reference VARCHAR(100),
    paid_at               TIMESTAMP WITH TIME ZONE,
    created_at            TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE TABLE recruitment_applications
(
    id             UUID PRIMARY KEY,
    candidate_id   UUID REFERENCES users (id) ON DELETE CASCADE       NOT NULL,
    field_of_study VARCHAR(150)                                       NOT NULL,
    degree_level   VARCHAR(50)                                        NOT NULL,
    status         recruitment_status       DEFAULT 'DRAFT'           NOT NULL,
    created_at     TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE TABLE ai_chat_sessions
(
    id         UUID PRIMARY KEY,
    user_id    UUID REFERENCES users (id) ON DELETE CASCADE       NOT NULL,
    title      VARCHAR(150)             DEFAULT 'New Chat'        NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE TABLE ai_chat_messages
(
    id          UUID PRIMARY KEY,
    session_id  UUID REFERENCES ai_chat_sessions (id) ON DELETE CASCADE NOT NULL,
    sender      VARCHAR(20)                                             NOT NULL,
    content     TEXT                                                    NOT NULL,
    rag_sources JSONB,
    created_at  TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP      NOT NULL
);

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

CREATE TABLE user_notifications
(
    id         UUID PRIMARY KEY,
    user_id    UUID REFERENCES users (id) ON DELETE CASCADE       NOT NULL,
    title      VARCHAR(200)                                       NOT NULL,
    body       TEXT                                               NOT NULL,
    category   VARCHAR(50)                                        NOT NULL, -- 'SCHEDULE', 'GRADE', 'PAYMENT', etc.
    is_read    BOOLEAN                  DEFAULT FALSE             NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE INDEX idx_user_notifications_unread ON user_notifications (user_id) WHERE is_read = FALSE;

CREATE TABLE outbox_events
(
    id             UUID PRIMARY KEY,                                            -- Monotonic UUIDv7
    aggregate_type VARCHAR(100)                                       NOT NULL, -- e.g., 'GRADE', 'SCHEDULE', 'SURVEY'
    aggregate_id   VARCHAR(100)                                       NOT NULL,
    event_type     VARCHAR(100)                                       NOT NULL, -- e.g., 'GradePublished', 'ScheduleUpdated'
    target_type    notification_target_type                           NOT NULL, -- TARGET_USER, TARGET_GROUP, etc.
    target_id      VARCHAR(100),                                                -- Nullable for global targets
    payload        JSONB                                              NOT NULL, -- Notification content, title, deep-links
    processed      BOOLEAN                  DEFAULT FALSE             NOT NULL,
    created_at     TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE INDEX idx_outbox_unprocessed_v7 ON outbox_events (id) WHERE processed = FALSE;