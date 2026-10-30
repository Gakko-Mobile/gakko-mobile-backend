CREATE TABLE survey_reminder_dispatches
(
    survey_id     UUID                                            NOT NULL REFERENCES surveys (id) ON DELETE CASCADE,
    interval_days NUMERIC                                         NOT NULL,
    dispatched_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,
    PRIMARY KEY (survey_id, interval_days)
);
