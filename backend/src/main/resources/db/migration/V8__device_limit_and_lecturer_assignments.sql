-- Keine bestehenden Geräte automatisch sperren: Mehrfachbindungen vor dem Deployment bereinigen.
DO $$
BEGIN
    IF EXISTS (SELECT user_id FROM devices WHERE status = 'ACTIVE' GROUP BY user_id HAVING count(*) > 1) THEN
        RAISE EXCEPTION 'Multiple active devices exist for a user; revoke surplus devices before deployment';
    END IF;
END;
$$;

CREATE UNIQUE INDEX uq_devices_single_active_user ON devices (user_id) WHERE status = 'ACTIVE';

DROP TRIGGER trg_single_active_student_device ON devices;
DROP FUNCTION enforce_single_active_student_device();
DROP TRIGGER trg_student_role_device_limit ON users;
DROP FUNCTION enforce_student_role_device_limit();

ALTER TABLE modules ADD COLUMN lecturer_user_id UUID REFERENCES users(id) ON DELETE SET NULL;
ALTER TABLE lectures ADD COLUMN lecturer_override BOOLEAN NOT NULL DEFAULT FALSE;

-- Zukünftige Kalender-Zuordnungen werden ab jetzt manuell im Sekretariat festgelegt.
UPDATE lectures SET lecturer_user_id = NULL
WHERE source = 'TRAINEX' AND starts_at > CURRENT_TIMESTAMP AND opened_at IS NULL;
