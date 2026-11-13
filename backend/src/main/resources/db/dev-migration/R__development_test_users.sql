INSERT INTO users (id, first_name, last_name, email, role, status)
VALUES ('10000000-0000-4000-8000-000000000001', 'George', 'Stobbart',
        'student1@dev.ba-check-in.test', 'STUDENT', 'ACTIVE'),
       ('10000000-0000-4000-8000-000000000002', 'Nicole', 'Collard',
        'student2@dev.ba-check-in.test', 'STUDENT', 'ACTIVE'),
       ('10000000-0000-4000-8000-000000000003', 'Alexander', 'Schottky',
        'student3@dev.ba-check-in.test', 'STUDENT', 'ACTIVE'),
       ('10000000-0000-4000-8000-000000000004', 'Darius', 'Dunkmaster',
        'student4@dev.ba-check-in.test', 'STUDENT', 'ACTIVE'),
       ('10000000-0000-4000-8000-000000000005', 'Lara', 'Croft',
        'student5@dev.ba-check-in.test', 'STUDENT', 'ACTIVE'),
       ('20000000-0000-4000-8000-000000000001', 'Markus', 'Rühl',
        'lecturer1@dev.ba-check-in.test', 'LECTURER', 'ACTIVE'),
       ('20000000-0000-4000-8000-000000000002', 'Lehrer', 'Schmidt',
        'lecturer2@dev.ba-check-in.test', 'LECTURER', 'ACTIVE'),
       ('20000000-0000-4000-8000-000000000003', '', 'Platon',
        'lecturer3@dev.ba-check-in.test', 'LECTURER', 'ACTIVE'),
       ('30000000-0000-4000-8000-000000000001', 'Sun', 'Tzu',
        'secretary1@dev.ba-check-in.test', 'SECRETARY', 'ACTIVE'),
       ('30000000-0000-4000-8000-000000000002', 'Napoleon', 'Bonaparte',
        'secretary2@dev.ba-check-in.test', 'SECRETARY', 'ACTIVE'),
       ('30000000-0000-4000-8000-000000000003', 'Jaques', 'de la Grande Canalisation',
        'secretary3@dev.ba-check-in.test', 'SECRETARY', 'ACTIVE')
ON CONFLICT DO NOTHING;

INSERT INTO course_groups (id, name, active)
VALUES ('40000000-0000-4000-8000-000000000001',
        'DEV-GROUP',
        TRUE)
ON CONFLICT DO NOTHING;

INSERT INTO students (id, user_id, matriculation_number, course_group_id)
VALUES ('50000000-0000-4000-8000-000000000001',
        '10000000-0000-4000-8000-000000000001',
        'DEV00001',
        '40000000-0000-4000-8000-000000000001'),
       ('50000000-0000-4000-8000-000000000002',
        '10000000-0000-4000-8000-000000000002',
        'DEV00002',
        '40000000-0000-4000-8000-000000000001'),
       ('50000000-0000-4000-8000-000000000003',
        '10000000-0000-4000-8000-000000000003',
        'DEV00003',
        '40000000-0000-4000-8000-000000000001'),
       ('50000000-0000-4000-8000-000000000004',
        '10000000-0000-4000-8000-000000000004',
        'DEV00004',
        '40000000-0000-4000-8000-000000000001'),
       ('50000000-0000-4000-8000-000000000005',
        '10000000-0000-4000-8000-000000000005',
        'DEV00005',
        '40000000-0000-4000-8000-000000000001')
ON CONFLICT DO NOTHING;

INSERT INTO nfc_tags (id, public_id, tag_uid, display_name, key_version, active)
VALUES ('60000000-0000-4000-8000-000000000001', '61000000-0000-4000-8000-000000000001', '04DE5F1EACC040', 'Dev NFC Tag 1', 'v1', TRUE),
       ('60000000-0000-4000-8000-000000000002', '61000000-0000-4000-8000-000000000002', '04A224B2D00001', 'Dev NFC Tag 2', 'v1', TRUE),
       ('60000000-0000-4000-8000-000000000003', '61000000-0000-4000-8000-000000000003', '04A224B3E10001', 'Dev NFC Tag 3', 'v1', TRUE)
ON CONFLICT (id) DO UPDATE
SET public_id = EXCLUDED.public_id,
    tag_uid = EXCLUDED.tag_uid,
    display_name = EXCLUDED.display_name,
    key_version = EXCLUDED.key_version,
    active = EXCLUDED.active;
