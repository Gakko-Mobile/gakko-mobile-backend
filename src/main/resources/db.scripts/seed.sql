-- PASSWORD FOR ALL USERS: "Password123!"



-- 1. STUDIES (Exactly 2 records)
INSERT INTO studies (id, name, created_at)
VALUES ('019ff6f0-0000-7000-8000-000000000011', 'Computer Science - Web Development', '2026-09-01 10:00:00+02'),
       ('019ff6f0-0000-7000-8000-000000000012', 'New Media Arts - Graphic Design', '2026-09-01 10:00:00+02');

-- 2. BUILDINGS (Exactly 2 records)
INSERT INTO buildings (id, name, created_at)
VALUES ('019ff6f0-0000-7000-8000-000000000021', 'Main Campus Building', '2026-09-01 10:00:00+02'),
       ('019ff6f0-0000-7000-8000-000000000022', 'Library & Science Center', '2026-09-01 10:00:00+02');

-- 3. USERS (10 records: 2 Lecturers, 8 Students)
INSERT INTO users (id, role, email, password_hash, first_name, last_name, index_number, mfa_enabled,
                   password_changed_at)
VALUES
    -- Lecturers
    ('019ff6f0-0000-7000-8000-000000000031', 'LECTURER', 'jan.kowalski@pjatk.edu.pl',
     '$2a$10$IUsYNbTaFf3OeCh6OonS2ed4IFgq20R5hhqL3A/NDzRWn5G386tMi', 'Jan', 'Kowalski', 'emp001', false,
     '2026-09-01 10:00:00+02'),
    ('019ff6f0-0000-7000-8000-000000000032', 'LECTURER', 'anna.nowak@pjatk.edu.pl',
     '$2a$10$IUsYNbTaFf3OeCh6OonS2ed4IFgq20R5hhqL3A/NDzRWn5G386tMi', 'Anna', 'Nowak', 'emp002', false,
     '2026-09-01 10:00:00+02'),
    -- Students (Including Danisa)
    ('019ff6f0-55ff-7bc9-8f81-5fb79d602115', 'STUDENT', 'danisa.novikova@gmail.com',
     '$2a$10$IUsYNbTaFf3OeCh6OonS2ed4IFgq20R5hhqL3A/NDzRWn5G386tMi', 'Danisa', 'Novikova', 's12345', false,
     '2026-08-12 19:06:13+02'),
    ('019ff6f0-0000-7000-8000-000000000034', 'STUDENT', 'michal.wisniewski@gmail.com',
     '$2a$10$IUsYNbTaFf3OeCh6OonS2ed4IFgq20R5hhqL3A/NDzRWn5G386tMi', 'Michal', 'Wisniewski', 's10004', false,
     '2026-09-01 10:00:00+02'),
    ('019ff6f0-0000-7000-8000-000000000035', 'STUDENT', 'kasia.lewandowska@gmail.com',
     '$2a$10$IUsYNbTaFf3OeCh6OonS2ed4IFgq20R5hhqL3A/NDzRWn5G386tMi', 'Kasia', 'Lewandowska', 's10005', false,
     '2026-09-01 10:00:00+02'),
    ('019ff6f0-0000-7000-8000-000000000036', 'STUDENT', 'piotr.zielinski@gmail.com',
     '$2a$10$IUsYNbTaFf3OeCh6OonS2ed4IFgq20R5hhqL3A/NDzRWn5G386tMi', 'Piotr', 'Zielinski', 's10006', false,
     '2026-09-01 10:00:00+02'),
    ('019ff6f0-0000-7000-8000-000000000037', 'STUDENT', 'maria.kaminska@gmail.com',
     '$2a$10$IUsYNbTaFf3OeCh6OonS2ed4IFgq20R5hhqL3A/NDzRWn5G386tMi', 'Maria', 'Kaminska', 's10007', false,
     '2026-09-01 10:00:00+02'),
    ('019ff6f0-0000-7000-8000-000000000038', 'STUDENT', 'tomasz.wojcik@gmail.com',
     '$2a$10$IUsYNbTaFf3OeCh6OonS2ed4IFgq20R5hhqL3A/NDzRWn5G386tMi', 'Tomasz', 'Wojcik', 's10008', false,
     '2026-09-01 10:00:00+02'),
    ('019ff6f0-0000-7000-8000-000000000039', 'STUDENT', 'ola.szymanska@gmail.com',
     '$2a$10$IUsYNbTaFf3OeCh6OonS2ed4IFgq20R5hhqL3A/NDzRWn5G386tMi', 'Ola', 'Szymanska', 's10009', false,
     '2026-09-01 10:00:00+02'),
    ('019ff6f0-0000-7000-8000-000000000040', 'STUDENT', 'kamil.dabrowski@gmail.com',
     '$2a$10$IUsYNbTaFf3OeCh6OonS2ed4IFgq20R5hhqL3A/NDzRWn5G386tMi', 'Kamil', 'Dabrowski', 's10010', false,
     '2026-09-01 10:00:00+02');

-- 4. ACADEMIC GROUPS (10 records: 5 for CS, 5 for Graphic Design)
INSERT INTO academic_groups (id, name, academic_year, studies_id)
VALUES ('019ff6f0-0000-7000-8000-000000000041', 'WIs I.1', '2026/2027','019ff6f0-0000-7000-8000-000000000011'),
       ('019ff6f0-0000-7000-8000-000000000042', 'WIs I.2', '2026/2027', '019ff6f0-0000-7000-8000-000000000011'),
       ('019ff6f0-0000-7000-8000-000000000043', 'WIs I.3', '2026/2027', '019ff6f0-0000-7000-8000-000000000011'),
       ('019ff6f0-0000-7000-8000-000000000044', 'WIs II.1', '2026/2027', '019ff6f0-0000-7000-8000-000000000011'),
       ('019ff6f0-0000-7000-8000-000000000045', 'WIs II.2', '2026/2027', '019ff6f0-0000-7000-8000-000000000011'),
       ('019ff6f0-0000-7000-8000-000000000046', 'SNM I.1', '2026/2027', '019ff6f0-0000-7000-8000-000000000012'),
       ('019ff6f0-0000-7000-8000-000000000047', 'SNM I.2', '2026/2027', '019ff6f0-0000-7000-8000-000000000012'),
       ('019ff6f0-0000-7000-8000-000000000048', 'SNM I.3', '2026/2027', '019ff6f0-0000-7000-8000-000000000012'),
       ('019ff6f0-0000-7000-8000-000000000049', 'SNM II.1', '2026/2027', '019ff6f0-0000-7000-8000-000000000012'),
       ('019ff6f0-0000-7000-8000-00000000004a', 'SNM II.2', '2026/2027', '019ff6f0-0000-7000-8000-000000000012');

-- 5. USER GROUPS (10 records: assigning users to groups. Danisa gets ITN status in one)
INSERT INTO user_groups (user_id, group_id, is_itn)
VALUES ('019ff6f0-0000-7000-8000-000000000031', '019ff6f0-0000-7000-8000-000000000041', false), -- Lecturer 1 in WIs I.1
       ('019ff6f0-0000-7000-8000-000000000032', '019ff6f0-0000-7000-8000-000000000046', false), -- Lecturer 2 in SNM I.1
       ('019ff6f0-55ff-7bc9-8f81-5fb79d602115', '019ff6f0-0000-7000-8000-000000000041',
        false),                                                                                 -- Danisa in WIs I.1 (Regular)
       ('019ff6f0-55ff-7bc9-8f81-5fb79d602115', '019ff6f0-0000-7000-8000-000000000044',
        true),                                                                                  -- Danisa in WIs II.1 (ITN status)
       ('019ff6f0-0000-7000-8000-000000000034', '019ff6f0-0000-7000-8000-000000000041', false),
       ('019ff6f0-0000-7000-8000-000000000035', '019ff6f0-0000-7000-8000-000000000042', false),
       ('019ff6f0-0000-7000-8000-000000000036', '019ff6f0-0000-7000-8000-000000000043', false),
       ('019ff6f0-0000-7000-8000-000000000037', '019ff6f0-0000-7000-8000-000000000046', false),
       ('019ff6f0-0000-7000-8000-000000000038', '019ff6f0-0000-7000-8000-000000000047', false),
       ('019ff6f0-0000-7000-8000-000000000039', '019ff6f0-0000-7000-8000-000000000048', false);

-- 6. ROOMS (10 records: 5 per building)
INSERT INTO rooms (id, name, building_id)
VALUES ('019ff6f0-0000-7000-8000-000000000051', 'Aula A', '019ff6f0-0000-7000-8000-000000000021'),
       ('019ff6f0-0000-7000-8000-000000000052', 'Aula B', '019ff6f0-0000-7000-8000-000000000021'),
       ('019ff6f0-0000-7000-8000-000000000053', 'Lab 101', '019ff6f0-0000-7000-8000-000000000021'),
       ('019ff6f0-0000-7000-8000-000000000054', 'Lab 102', '019ff6f0-0000-7000-8000-000000000021'),
       ('019ff6f0-0000-7000-8000-000000000055', 'Lab 103', '019ff6f0-0000-7000-8000-000000000021'),
       ('019ff6f0-0000-7000-8000-000000000056', 'Art Studio 1', '019ff6f0-0000-7000-8000-000000000022'),
       ('019ff6f0-0000-7000-8000-000000000057', 'Art Studio 2', '019ff6f0-0000-7000-8000-000000000022'),
       ('019ff6f0-0000-7000-8000-000000000058', 'Mac Lab A', '019ff6f0-0000-7000-8000-000000000022'),
       ('019ff6f0-0000-7000-8000-000000000059', 'Mac Lab B', '019ff6f0-0000-7000-8000-000000000022'),
       ('019ff6f0-0000-7000-8000-00000000005a', 'Seminar Room', '019ff6f0-0000-7000-8000-000000000022');

-- 7. SUBJECTS (10 records)
INSERT INTO subjects (id, code, name, type, ects_points)
VALUES ('019ff6f0-0000-7000-8000-000000000061', 'PRG', 'Advanced Java Programming', 'LECTURE', 5),
       ('019ff6f0-0000-7000-8000-000000000062', 'WEB', 'React & TypeScript Frontend', 'TUTORIAL', 6),
       ('019ff6f0-0000-7000-8000-000000000063', 'DB', 'Database Systems', 'LECTURE', 5),
       ('019ff6f0-0000-7000-8000-000000000064', 'ALG', 'Algorithms & Data Structures', 'TUTORIAL', 6),
       ('019ff6f0-0000-7000-8000-000000000065', 'SEC', 'Cybersecurity Basics', 'LECTURE', 4),
       ('019ff6f0-0000-7000-8000-000000000066', 'UIX', 'UI/UX Design Principles', 'LECTURE', 4),
       ('019ff6f0-0000-7000-8000-000000000067', 'TYP', 'Typography in Digital Media', 'TUTORIAL', 5),
       ('019ff6f0-0000-7000-8000-000000000068', '3DM', '3D Modeling with Blender', 'TUTORIAL', 6),
       ('019ff6f0-0000-7000-8000-000000000069', 'ENG', 'English B22', 'LANGUAGE_COURSE', 3),
       ('019ff6f0-0000-7000-8000-00000000006a', 'DIP', 'Diploma Seminar', 'DIPLOMA_PROJECT', 10);

-- 8. GROUP CLASSES (10 records scheduled for today/tomorrow)
INSERT INTO group_classes (id, subject_id, room_id, start_timestamp, end_timestamp, ms_teams_code)
VALUES
    -- Classes for October 3, 2026 (For testing the getDailySchedule method)
    ('019ff6f0-0000-7000-8000-000000000071', '019ff6f0-0000-7000-8000-000000000061',
     '019ff6f0-0000-7000-8000-000000000051', '2026-10-03 08:15:00+02', '2026-10-03 09:45:00+02', 'teams-prg-1'),
    ('019ff6f0-0000-7000-8000-000000000072', '019ff6f0-0000-7000-8000-000000000062',
     '019ff6f0-0000-7000-8000-000000000053', '2026-10-03 10:00:00+02', '2026-10-03 11:30:00+02', null),
    ('019ff6f0-0000-7000-8000-000000000073', '019ff6f0-0000-7000-8000-000000000063',
     '019ff6f0-0000-7000-8000-000000000052', '2026-10-03 11:45:00+02', '2026-10-03 13:15:00+02', null),
    ('019ff6f0-0000-7000-8000-000000000074', '019ff6f0-0000-7000-8000-000000000069',
     '019ff6f0-0000-7000-8000-00000000005a', '2026-10-03 13:30:00+02', '2026-10-03 15:00:00+02', 'teams-eng-1'),
    ('019ff6f0-0000-7000-8000-000000000075', '019ff6f0-0000-7000-8000-000000000066',
     '019ff6f0-0000-7000-8000-000000000056', '2026-10-03 08:15:00+02', '2026-10-03 09:45:00+02', null),
    -- Classes for October 4, 2026
    ('019ff6f0-0000-7000-8000-000000000076', '019ff6f0-0000-7000-8000-000000000064',
     '019ff6f0-0000-7000-8000-000000000054', '2026-10-04 08:15:00+02', '2026-10-04 09:45:00+02', null),
    ('019ff6f0-0000-7000-8000-000000000077', '019ff6f0-0000-7000-8000-000000000065',
     '019ff6f0-0000-7000-8000-000000000051', '2026-10-04 10:00:00+02', '2026-10-04 11:30:00+02', null),
    ('019ff6f0-0000-7000-8000-000000000078', '019ff6f0-0000-7000-8000-000000000067',
     '019ff6f0-0000-7000-8000-000000000058', '2026-10-04 11:45:00+02', '2026-10-04 13:15:00+02', null),
    ('019ff6f0-0000-7000-8000-000000000079', '019ff6f0-0000-7000-8000-000000000068',
     '019ff6f0-0000-7000-8000-000000000059', '2026-10-04 13:30:00+02', '2026-10-04 15:00:00+02', null),
    ('019ff6f0-0000-7000-8000-00000000007a', '019ff6f0-0000-7000-8000-00000000006a',
     '019ff6f0-0000-7000-8000-00000000005a', '2026-10-04 15:15:00+02', '2026-10-04 16:45:00+02', 'teams-dip-1');

-- 9. ACADEMIC GROUPS -> GROUP CLASSES (10 records: Bridge Table)
INSERT INTO academic_groups_group_classes (class_id, group_id)
VALUES
    -- Mapping Danisa's primary group (WIs I.1) to several classes
    ('019ff6f0-0000-7000-8000-000000000071', '019ff6f0-0000-7000-8000-000000000041'),
    ('019ff6f0-0000-7000-8000-000000000072', '019ff6f0-0000-7000-8000-000000000041'),
    ('019ff6f0-0000-7000-8000-000000000073', '019ff6f0-0000-7000-8000-000000000041'),

    -- Mapping Danisa's ITN group (WIs II.1) to an advanced class
    ('019ff6f0-0000-7000-8000-000000000077', '019ff6f0-0000-7000-8000-000000000044'),

    -- Mapping other groups to classes
    ('019ff6f0-0000-7000-8000-000000000074', '019ff6f0-0000-7000-8000-000000000042'),
    ('019ff6f0-0000-7000-8000-000000000075', '019ff6f0-0000-7000-8000-000000000046'),
    ('019ff6f0-0000-7000-8000-000000000076', '019ff6f0-0000-7000-8000-000000000043'),
    ('019ff6f0-0000-7000-8000-000000000078', '019ff6f0-0000-7000-8000-000000000047'),
    ('019ff6f0-0000-7000-8000-000000000079', '019ff6f0-0000-7000-8000-000000000048'),
    ('019ff6f0-0000-7000-8000-00000000007a', '019ff6f0-0000-7000-8000-000000000049');

UPDATE users SET password_hash = '$2a$10$SHOpK0jnkFcimNVcZyC3OeQcihnW6DWAnmQUjAyqIvYnUN/j5Wc7e';
UPDATE users SET index_number = null where role != 'STUDENT';

-- 10. EVENTS (10 records scheduled across various dates, including test dates)
INSERT INTO events (id, organizator, description, building_id, start_timestamp, end_timestamp, created_at)
VALUES
    -- Events on October 3, 2026 (Test Date)
    ('019ff6f0-0000-7000-8000-000000000081', 'Career Services', 'Tech Industry Resume Workshop',
     '019ff6f0-0000-7000-8000-000000000021', '2026-10-03 16:00:00+02', '2026-10-03 18:00:00+02', '2026-09-15 10:00:00+02'),
    ('019ff6f0-0000-7000-8000-000000000082', 'Student Board', 'Welcome Semester Integration Party',
     '019ff6f0-0000-7000-8000-000000000022', '2026-10-03 19:00:00+02', '2026-10-03 23:00:00+02', '2026-09-15 10:00:00+02'),

    -- Events on October 4, 2026 (Test Date)
    ('019ff6f0-0000-7000-8000-000000000083', 'IT Faculty', 'Guest Lecture: Future of AI in Web Development',
     '019ff6f0-0000-7000-8000-000000000021', '2026-10-04 12:00:00+02', '2026-10-04 14:00:00+02', '2026-09-15 10:00:00+02'),
    ('019ff6f0-0000-7000-8000-000000000084', 'Game Dev Club', 'Hackathon 2026 Kickoff and Team Building',
     '019ff6f0-0000-7000-8000-000000000022', '2026-10-04 17:00:00+02', '2026-10-04 20:00:00+02', '2026-09-15 10:00:00+02'),

    -- Upcoming Events
    ('019ff6f0-0000-7000-8000-000000000085', 'Academic Council', 'Open Q&A Session with the Dean',
     '019ff6f0-0000-7000-8000-000000000021', '2026-10-10 10:00:00+02', '2026-10-10 11:30:00+02', '2026-09-15 10:00:00+02'),
    ('019ff6f0-0000-7000-8000-000000000086', 'Erasmus Office', 'Study Abroad 2027 Information Session',
     '019ff6f0-0000-7000-8000-000000000022', '2026-10-12 13:00:00+02', '2026-10-12 15:00:00+02', '2026-09-15 10:00:00+02'),
    ('019ff6f0-0000-7000-8000-000000000087', 'Cybersecurity Club', 'Capture The Flag (CTF) Training #1',
     '019ff6f0-0000-7000-8000-000000000021', '2026-10-15 18:00:00+02', '2026-10-15 21:00:00+02', '2026-09-15 10:00:00+02'),
    ('019ff6f0-0000-7000-8000-000000000088', 'Alumni Association', 'Tech Networking Evening',
     '019ff6f0-0000-7000-8000-000000000022', '2026-10-20 18:30:00+02', '2026-10-20 21:30:00+02', '2026-09-15 10:00:00+02'),
    ('019ff6f0-0000-7000-8000-000000000089', 'Library Services', 'How to Write Your Thesis using LaTeX',
     '019ff6f0-0000-7000-8000-000000000022', '2026-10-22 14:00:00+02', '2026-10-22 16:00:00+02', '2026-09-15 10:00:00+02'),
    ('019ff6f0-0000-7000-8000-00000000008a', 'E-Sports Team', 'PJATK League of Legends Finals Watch Party',
     '019ff6f0-0000-7000-8000-000000000021', '2026-10-25 17:00:00+02', '2026-10-25 22:00:00+02', '2026-09-15 10:00:00+02');