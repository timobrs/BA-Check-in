ALTER TABLE modules
    ADD COLUMN ignored BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE lectures
    ADD COLUMN ignored BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE attendance_events
    DROP CONSTRAINT chk_attendance_events_event_type;

ALTER TABLE attendance_events
    DROP COLUMN event_type;
