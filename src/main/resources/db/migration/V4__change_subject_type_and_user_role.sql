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