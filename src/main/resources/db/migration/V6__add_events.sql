CREATE TABLE events
(
    id              UUID PRIMARY KEY,
    organizator     VARCHAR(100)                                       NOT NULL,
    description     TEXT                                               NOT NULL,
    building_id     UUID REFERENCES buildings (id) ON DELETE CASCADE   NOT NULL,
    start_timestamp TIMESTAMP WITH TIME ZONE                           NOT NULL,
    end_timestamp   TIMESTAMP WITH TIME ZONE                           NOT NULL,
    created_at      TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL
);