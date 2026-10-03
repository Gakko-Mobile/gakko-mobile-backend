-- change subject_type

ALTER TYPE subject_type RENAME TO subject_type_old;

CREATE TYPE subject_type AS ENUM ('LECTURE', 'TUTORIAL', 'LANGUAGE_COURSE', 'DIPLOMA_PROJECT');

ALTER TABLE subjects
ALTER
COLUMN "type" TYPE subject_type
  USING (
    CASE "type"::text
      WHEN 'LECTURE' THEN 'LECTURE'
      WHEN 'LABORATORY' THEN 'TUTORIAL'
      WHEN 'EXERCISE' THEN 'TUTORIAL'
      WHEN 'PROJECT' THEN 'DIPLOMA_PROJECT'
      WHEN 'SEMINAR' THEN 'TUTORIAL'
      ELSE 'LECTURE'
    END
  )::subject_type;

DROP TYPE subject_type_old;

-- change user_role

ALTER TYPE user_role RENAME TO user_role_old;

CREATE TYPE user_role AS ENUM ('STUDENT', 'EMPLOYEE', 'USER', 'ADMIN', 'LECTURER');

ALTER TABLE users
ALTER
COLUMN "role" TYPE user_role
  USING (
    CASE "role"::text
      WHEN 'STUDENT' THEN 'STUDENT'
      WHEN 'EMPLOYEE' THEN 'EMPLOYEE'
      WHEN 'CANDIDATE' THEN 'USER'
      WHEN 'ADMIN' THEN 'ADMIN'
      ELSE 'USER'
    END
  )::user_role;

DROP TYPE user_role_old;

-- !!!!!!!!!!!!!

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

-- !!!!!!!!!!!!!!

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

-- !!!!!!!!!!!!!!

CREATE TABLE studies
(
    id         UUID PRIMARY KEY,
    name       VARCHAR(100) UNIQUE                                NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL
);

ALTER TABLE academic_groups
    ADD COLUMN studies_id UUID REFERENCES studies (id) ON DELETE CASCADE NOT NULL;

-- !!!!!!!!!!!!!

ALTER TABLE user_groups
    ADD COLUMN is_itn BOOLEAN DEFAULT FALSE NOT NULL;
ALTER TABLE user_groups
    ADD COLUMN joined_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL;