ALTER TABLE nfc_tags
    ADD COLUMN public_id UUID NOT NULL DEFAULT gen_random_uuid(),
    ADD COLUMN key_version VARCHAR(50) NOT NULL DEFAULT 'v1',
    ADD COLUMN last_seen_at TIMESTAMPTZ;

ALTER TABLE nfc_tags
    ADD CONSTRAINT uq_nfc_tags_public_id UNIQUE (public_id);

CREATE TABLE modules
(
    id              UUID PRIMARY KEY      DEFAULT gen_random_uuid(),
    course_group_id UUID         NOT NULL,
    name            VARCHAR(255) NOT NULL,
    normalized_name VARCHAR(255) NOT NULL,
    active          BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_modules_course_group FOREIGN KEY (course_group_id)
        REFERENCES course_groups (id) ON DELETE CASCADE,
    CONSTRAINT uq_modules_course_normalized_name UNIQUE (course_group_id, normalized_name)
);

INSERT INTO modules (course_group_id, name, normalized_name)
SELECT DISTINCT ON (course_group_id, lower(trim(regexp_replace(title, '[^[:alnum:]]+', ' ', 'g'))))
       course_group_id,
       title,
       lower(trim(regexp_replace(title, '[^[:alnum:]]+', ' ', 'g')))
FROM lectures
ORDER BY course_group_id, lower(trim(regexp_replace(title, '[^[:alnum:]]+', ' ', 'g'))), created_at;

ALTER TABLE lectures
    ADD COLUMN module_id UUID,
    ADD COLUMN last_punctual_check_in_at TIMESTAMPTZ,
    ADD COLUMN late_phase_started_at TIMESTAMPTZ,
    ADD COLUMN attendance_finalized_at TIMESTAMPTZ;

UPDATE lectures lecture
SET module_id = module.id
FROM modules module
WHERE module.course_group_id = lecture.course_group_id
  AND module.normalized_name = lower(trim(regexp_replace(lecture.title, '[^[:alnum:]]+', ' ', 'g')));

ALTER TABLE lectures
    ALTER COLUMN module_id SET NOT NULL,
    ADD CONSTRAINT fk_lectures_module FOREIGN KEY (module_id)
        REFERENCES modules (id) ON DELETE RESTRICT;

CREATE INDEX idx_lectures_module_id ON lectures (module_id);
CREATE INDEX idx_lectures_unfinalized ON lectures (ends_at) WHERE attendance_finalized_at IS NULL;

CREATE TABLE student_module_enrollments
(
    id          UUID PRIMARY KEY     DEFAULT gen_random_uuid(),
    student_id  UUID        NOT NULL,
    module_id   UUID        NOT NULL,
    assigned_by UUID        NOT NULL,
    assigned_at TIMESTAMPTZ NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_student_module_enrollments_student FOREIGN KEY (student_id)
        REFERENCES students (id) ON DELETE CASCADE,
    CONSTRAINT fk_student_module_enrollments_module FOREIGN KEY (module_id)
        REFERENCES modules (id) ON DELETE CASCADE,
    CONSTRAINT fk_student_module_enrollments_actor FOREIGN KEY (assigned_by)
        REFERENCES users (id) ON DELETE RESTRICT,
    CONSTRAINT uq_student_module_enrollments UNIQUE (student_id, module_id)
);

CREATE INDEX idx_student_module_enrollments_student ON student_module_enrollments (student_id);
CREATE INDEX idx_student_module_enrollments_module ON student_module_enrollments (module_id);

UPDATE attendance_records SET status = 'PARTIALLY' WHERE status = 'LEFT_EARLY';
UPDATE attendance_records SET status = 'MANUAL_REVIEW' WHERE status = 'SICK';
ALTER TABLE attendance_records DROP CONSTRAINT chk_attendance_records_status;
ALTER TABLE attendance_records ADD CONSTRAINT chk_attendance_records_status
    CHECK (status IN ('PRESENT', 'LATE', 'PARTIALLY', 'ABSENT', 'EXCUSED', 'MANUAL_REVIEW'));

ALTER TABLE attendance_events ALTER COLUMN nfc_tag_id DROP NOT NULL;
ALTER TABLE attendance_events DROP CONSTRAINT chk_attendance_events_validation_status;
ALTER TABLE attendance_events ADD CONSTRAINT chk_attendance_events_validation_status CHECK (validation_status IN (
    'VALID', 'INVALID', 'REPLAY_DETECTED', 'DEVICE_NOT_APPROVED', 'DEVICE_REVOKED',
    'INVALID_SIGNATURE', 'UNKNOWN_TAG', 'CARD_INACTIVE', 'NO_MATCHING_LECTURE',
    'OVERLAPPING_LECTURES', 'ABSENCE_ACTIVE', 'ALREADY_CHECKED_IN'
));

ALTER TABLE sick_notes RENAME TO absence_reports;
ALTER INDEX idx_sick_notes_student_id RENAME TO idx_absence_reports_student_id;
ALTER INDEX idx_sick_notes_status RENAME TO idx_absence_reports_status;
ALTER INDEX idx_sick_notes_instant_range RENAME TO idx_absence_reports_instant_range;
ALTER TABLE absence_reports RENAME CONSTRAINT fk_sick_notes_student TO fk_absence_reports_student;
ALTER TABLE absence_reports RENAME CONSTRAINT fk_sick_notes_reviewed_by TO fk_absence_reports_reviewed_by;
ALTER TABLE absence_reports RENAME CONSTRAINT chk_sick_notes_status TO chk_absence_reports_status;
ALTER TABLE absence_reports RENAME CONSTRAINT chk_sick_notes_instant_range TO chk_absence_reports_instant_range;
