CREATE TABLE buildings
(
    id         UUID PRIMARY KEY,
    name       VARCHAR(50) UNIQUE                                 NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL
);


CREATE TABLE rooms
(
    id          UUID PRIMARY KEY,
    name        VARCHAR(50)                                        NOT NULL,
    building_id UUID REFERENCES buildings (id) ON DELETE RESTRICT  NOT NULL,
    created_at  TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL,

    UNIQUE (name, building_id)
);

CREATE TABLE group_classes
(
    id              UUID PRIMARY KEY,
    subject_id      UUID REFERENCES subjects (id) ON DELETE CASCADE    NOT NULL,
    room_id         UUID REFERENCES rooms (id) ON DELETE CASCADE       NOT NULL,
    start_timestamp TIMESTAMP WITH TIME ZONE                           NOT NULL,
    end_timestamp   TIMESTAMP WITH TIME ZONE                           NOT NULL,
    ms_teams_code   VARCHAR(100),
    created_at      TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE TABLE academic_groups_group_classes
(
    class_id UUID REFERENCES group_classes (id) ON DELETE CASCADE,
    group_id UUID REFERENCES academic_groups (id) ON DELETE CASCADE,
    PRIMARY KEY (class_id, group_id)
);

CREATE TABLE studies
(
    id         UUID PRIMARY KEY,
    name       VARCHAR(100) UNIQUE                                NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL
);

ALTER TABLE academic_groups
    ADD COLUMN studies_id UUID REFERENCES studies (id) ON DELETE CASCADE NOT NULL;

ALTER TABLE user_groups
    ADD COLUMN is_itn BOOLEAN DEFAULT FALSE NOT NULL;
ALTER TABLE user_groups
    ADD COLUMN joined_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL;

ALTER TABLE academic_groups
    DROP CONSTRAINT academic_groups_name_key;

ALTER TABLE academic_groups
    DROP COLUMN semester;