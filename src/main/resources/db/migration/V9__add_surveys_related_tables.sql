-- 1. Main Surveys Table
CREATE TABLE surveys
(
    id                       UUID PRIMARY KEY,                                  -- Time-ordered UUIDv7 generated application-side
    creator_id               UUID REFERENCES users (id) ON DELETE SET NULL,     -- Creator staff reference
    title                    VARCHAR(255)                               NOT NULL,
    description              TEXT,
    questions                JSONB                                      NOT NULL, -- Schema: SINGLE, MULTIPLE, TEXT, SCALE & Logic rules
    status                   VARCHAR(50)      DEFAULT 'DRAFT'           NOT NULL, -- SURVEY_STATUS: 'DRAFT', 'COLLECTING', 'CLOSED'
    target_type              VARCHAR(50)      DEFAULT 'ALL'             NOT NULL, -- AUDIENCE_TARGET_TYPE: 'ALL', 'GROUPS', 'COURSE', 'VIA_LINK'
    target_ids               JSONB,                                               -- Array of group or course IDs (e.g. ["CS L1 - 111I"])
    is_anonymous             BOOLEAN          DEFAULT TRUE              NOT NULL, -- Privacy toggle
    allow_resubmission       BOOLEAN          DEFAULT FALSE             NOT NULL, -- Toggle to permit updating responses
    require_login            BOOLEAN          DEFAULT TRUE              NOT NULL, -- Requires authenticated university account
    show_results_to_students BOOLEAN          DEFAULT FALSE             NOT NULL, -- Aggregated outcome charts visibility
    randomize_question_order BOOLEAN          DEFAULT FALSE             NOT NULL, -- Shuffle question order on render
    response_limit           INTEGER,                                             -- Max total responses before auto-closing
    confirmation_message     TEXT,                                                -- Custom thank you message
    opens_at                 TIMESTAMP WITH TIME ZONE,                            -- Scheduled opening time
    closes_at                TIMESTAMP WITH TIME ZONE,                            -- Scheduled closing time
    reminder_intervals       JSONB,                                               -- Array of reminder offsets before closing (e.g. [3, 1, 0.25, 0.0416])
    deleted_at               TIMESTAMP WITH TIME ZONE,                            -- Soft-delete timestamp (WHERE deleted_at IS NULL)
    created_at               TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at               TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL
);

-- Indexes for active survey filtering, creator queries, and JSONB searching
CREATE INDEX idx_surveys_creator ON surveys (creator_id);
CREATE INDEX idx_surveys_status_active ON surveys (status, closes_at) WHERE deleted_at IS NULL;
CREATE INDEX idx_surveys_questions_gin ON surveys USING GIN (questions);
CREATE INDEX idx_surveys_target_ids_gin ON surveys USING GIN (target_ids);

-- 2. Survey Completions Registry (Prevents Duplicate Submissions & Tracks Completion per Student)
CREATE TABLE survey_completions
(
    id           UUID PRIMARY KEY,                                              -- UUIDv7
    survey_id    UUID REFERENCES surveys (id) ON DELETE CASCADE            NOT NULL,
    user_id      UUID REFERENCES users (id) ON DELETE CASCADE              NOT NULL,
    completed_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP        NOT NULL,
    CONSTRAINT uk_survey_user_completion UNIQUE (survey_id, user_id)
);

CREATE INDEX idx_survey_completions_user ON survey_completions (user_id);
CREATE INDEX idx_survey_completions_survey ON survey_completions (survey_id);

-- 3. Survey Responses Table
CREATE TABLE survey_responses
(
    id           UUID PRIMARY KEY,                                              -- UUIDv7
    survey_id    UUID REFERENCES surveys (id) ON DELETE CASCADE            NOT NULL,
    user_id      UUID REFERENCES users (id) ON DELETE SET NULL,              -- Set to NULL when is_anonymous = TRUE
    answers      JSONB                                                     NOT NULL, -- Map of question_id -> answers payload
    submitted_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP        NOT NULL
);

CREATE INDEX idx_survey_responses_survey ON survey_responses (survey_id);
CREATE INDEX idx_survey_responses_answers_gin ON survey_responses USING GIN (answers);