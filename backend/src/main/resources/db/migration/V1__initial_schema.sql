CREATE
EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE users
(
    id         UUID PRIMARY KEY      DEFAULT gen_random_uuid(),
    first_name VARCHAR(100),
    last_name  VARCHAR(100) NOT NULL,
    email      VARCHAR(255) NOT NULL UNIQUE,
    role       VARCHAR(50)  NOT NULL,
    status     VARCHAR(50)  NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_users_role
        CHECK (role IN ('STUDENT', 'LECTURER', 'SECRETARY')),

    CONSTRAINT chk_users_status
        CHECK (status IN ('ACTIVE', 'INACTIVE', 'LOCKED'))
);

CREATE INDEX idx_users_role ON users (role);


CREATE TABLE course_groups
(
    id          UUID PRIMARY KEY      DEFAULT gen_random_uuid(),
    name        VARCHAR(100) NOT NULL UNIQUE,
    description TEXT,
    active      BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP
);


CREATE TABLE students
(
    id                   UUID PRIMARY KEY     DEFAULT gen_random_uuid(),

    user_id              UUID        NOT NULL UNIQUE,
    matriculation_number VARCHAR(50) NOT NULL UNIQUE,
    course_group_id      UUID        NOT NULL,

    created_at           TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at           TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_students_user
        FOREIGN KEY (user_id)
            REFERENCES users (id)
            ON DELETE CASCADE,

    CONSTRAINT fk_students_course_group
        FOREIGN KEY (course_group_id)
            REFERENCES course_groups (id)
            ON DELETE RESTRICT
);

CREATE INDEX idx_students_course_group_id ON students (course_group_id);


CREATE TABLE devices
(
    id                     UUID PRIMARY KEY     DEFAULT gen_random_uuid(),
    user_id                UUID        NOT NULL,
    device_name            VARCHAR(255) NOT NULL,
    platform               VARCHAR(50) NOT NULL,
    manufacturer           VARCHAR(100),
    model                  VARCHAR(100),
    os_version             VARCHAR(100),
    app_version            VARCHAR(100),
    status                 VARCHAR(50) NOT NULL DEFAULT 'PENDING_APPROVAL',
    approved_by            UUID,
    approved_at            TIMESTAMPTZ,
    revoked_by             UUID,
    revoked_at             TIMESTAMPTZ,
    revoke_reason          TEXT,
    created_at             TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at             TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_devices_user
        FOREIGN KEY (user_id)
            REFERENCES users (id)
            ON DELETE CASCADE,

    CONSTRAINT fk_devices_approved_by
        FOREIGN KEY (approved_by)
            REFERENCES users (id)
            ON DELETE SET NULL,

    CONSTRAINT fk_devices_revoked_by
        FOREIGN KEY (revoked_by)
            REFERENCES users (id)
            ON DELETE SET NULL,

    CONSTRAINT chk_devices_platform
        CHECK (platform IN ('IOS', 'ANDROID')),

    CONSTRAINT chk_devices_status
        CHECK (status IN ('PENDING_APPROVAL', 'ACTIVE', 'REVOKED', 'BLOCKED', 'REPLACED')),

    CONSTRAINT chk_devices_active_approval
        CHECK (status <> 'ACTIVE' OR approved_at IS NOT NULL),

    CONSTRAINT chk_devices_terminal_revocation
        CHECK (
            status NOT IN ('REVOKED', 'REPLACED')
            OR (revoked_at IS NOT NULL AND revoke_reason IS NOT NULL)
        )
);

CREATE INDEX idx_devices_user_id ON devices (user_id);
CREATE INDEX idx_devices_status ON devices (status);

CREATE OR REPLACE FUNCTION enforce_single_active_student_device()
    RETURNS TRIGGER
    LANGUAGE plpgsql
AS
$$
DECLARE
    owner_role VARCHAR(50);
BEGIN
    SELECT role
    INTO owner_role
    FROM users
    WHERE id = NEW.user_id
    FOR UPDATE;

    IF owner_role = 'STUDENT'
        AND EXISTS (
            SELECT 1
            FROM devices
            WHERE user_id = NEW.user_id
              AND status = 'ACTIVE'
              AND id <> NEW.id
        )
    THEN
        RAISE EXCEPTION 'A student may only have one active device'
            USING ERRCODE = '23505';
    END IF;
    RETURN NEW;
END;
$$;

CREATE TRIGGER trg_single_active_student_device
    BEFORE INSERT OR UPDATE OF user_id, status
    ON devices
    FOR EACH ROW
    WHEN (NEW.status = 'ACTIVE')
EXECUTE FUNCTION enforce_single_active_student_device();

CREATE OR REPLACE FUNCTION enforce_student_role_device_limit()
    RETURNS TRIGGER
    LANGUAGE plpgsql
AS
$$
BEGIN
    IF NEW.role = 'STUDENT'
        AND OLD.role <> 'STUDENT'
        AND (
            SELECT COUNT(*)
            FROM devices
            WHERE user_id = NEW.id
              AND status = 'ACTIVE'
        ) > 1
    THEN
        RAISE EXCEPTION 'A user with multiple active devices cannot become a student'
            USING ERRCODE = '23514';
    END IF;
    RETURN NEW;
END;
$$;

CREATE TRIGGER trg_student_role_device_limit
    BEFORE UPDATE OF role
    ON users
    FOR EACH ROW
EXECUTE FUNCTION enforce_student_role_device_limit();


CREATE TABLE device_credentials
(
    id                     UUID PRIMARY KEY     DEFAULT gen_random_uuid(),
    device_id              UUID        NOT NULL,
    public_key             TEXT        NOT NULL,
    public_key_fingerprint BYTEA       NOT NULL,
    algorithm              VARCHAR(50) NOT NULL DEFAULT 'ES256',
    active                 BOOLEAN     NOT NULL DEFAULT TRUE,
    session_version        BIGINT      NOT NULL DEFAULT 0,
    created_at             TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_used_at           TIMESTAMPTZ,

    CONSTRAINT fk_device_credentials_device
        FOREIGN KEY (device_id)
            REFERENCES devices (id)
            ON DELETE CASCADE,

    CONSTRAINT uq_device_credentials_public_key_fingerprint
        UNIQUE (public_key_fingerprint),

    CONSTRAINT chk_device_credentials_fingerprint_length
        CHECK (octet_length(public_key_fingerprint) = 32),

    CONSTRAINT chk_device_credentials_algorithm
        CHECK (algorithm = 'ES256'),

    CONSTRAINT chk_device_credentials_session_version
        CHECK (session_version >= 0)
);

CREATE INDEX idx_device_credentials_device_id
    ON device_credentials (device_id);

CREATE INDEX idx_device_credentials_active
    ON device_credentials (active);

CREATE UNIQUE INDEX idx_device_credentials_one_active_per_device
    ON device_credentials (device_id) WHERE active = TRUE;


CREATE TABLE device_attestations
(
    id                    UUID PRIMARY KEY     DEFAULT gen_random_uuid(),
    device_credential_id  UUID        NOT NULL UNIQUE,
    verification_status   VARCHAR(50)  NOT NULL,
    security_level        VARCHAR(50),
    verifier              VARCHAR(100) NOT NULL,
    certificate_chain     TEXT,
    play_integrity_status VARCHAR(50),
    verified_at           TIMESTAMPTZ,
    created_at            TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,

    FOREIGN KEY (device_credential_id)
        REFERENCES device_credentials (id)
        ON DELETE CASCADE,

    CONSTRAINT chk_device_attestations_verification_status
        CHECK (verification_status IN ('PENDING', 'VERIFIED', 'REJECTED', 'ERROR')),

    CONSTRAINT chk_device_attestations_security_level
        CHECK (
            security_level IS NULL
            OR security_level IN ('SOFTWARE', 'TEE', 'STRONGBOX', 'UNKNOWN')
        ),

    CONSTRAINT chk_device_attestations_play_integrity_status
        CHECK (
            play_integrity_status IS NULL
            OR play_integrity_status IN ('NOT_CHECKED', 'PASSED', 'FAILED', 'UNAVAILABLE')
        ),

    CONSTRAINT chk_device_attestations_verified_state
        CHECK (
            verification_status <> 'VERIFIED'
            OR (security_level IS NOT NULL AND verified_at IS NOT NULL)
        )
);


CREATE TABLE activation_codes
(
    id              UUID PRIMARY KEY     DEFAULT gen_random_uuid(),
    user_id         UUID        NOT NULL,
    created_by      UUID        NOT NULL,
    code_hash       BYTEA       NOT NULL UNIQUE,
    mode            VARCHAR(50) NOT NULL,
    expires_at      TIMESTAMPTZ NOT NULL,
    consumed_at     TIMESTAMPTZ,
    revoked_at      TIMESTAMPTZ,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_activation_codes_user
        FOREIGN KEY (user_id)
            REFERENCES users (id)
            ON DELETE CASCADE,

    CONSTRAINT fk_activation_codes_created_by
        FOREIGN KEY (created_by)
            REFERENCES users (id)
            ON DELETE RESTRICT,

    CONSTRAINT chk_activation_codes_hash_length
        CHECK (octet_length(code_hash) = 32),

    CONSTRAINT chk_activation_codes_mode
        CHECK (mode IN ('ADD_DEVICE', 'REPLACE_DEVICE')),

    CONSTRAINT chk_activation_codes_expiry
        CHECK (expires_at > created_at),

    CONSTRAINT chk_activation_codes_terminal_state
        CHECK (consumed_at IS NULL OR revoked_at IS NULL)
);

CREATE INDEX idx_activation_codes_user_id
    ON activation_codes (user_id);

CREATE INDEX idx_activation_codes_open
    ON activation_codes (user_id, expires_at) WHERE consumed_at IS NULL
      AND revoked_at IS NULL;

CREATE INDEX idx_activation_codes_expires_at
    ON activation_codes (expires_at);

CREATE UNIQUE INDEX idx_activation_codes_one_open_per_user
    ON activation_codes (user_id)
    WHERE consumed_at IS NULL AND revoked_at IS NULL;


CREATE TABLE activation_sessions
(
    id                          UUID PRIMARY KEY     DEFAULT gen_random_uuid(),
    challenge                   BYTEA       NOT NULL UNIQUE,
    expires_at                  TIMESTAMPTZ NOT NULL,
    consumed_at                 TIMESTAMPTZ,
    invalidated_at              TIMESTAMPTZ,
    activation_code_id          UUID        NOT NULL,
    result_device_credential_id UUID,
    created_at                  TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_activation_sessions_expiry
        CHECK (expires_at > created_at),

    CONSTRAINT chk_activation_sessions_challenge_length
        CHECK (octet_length(challenge) = 32),

    CONSTRAINT fk_activation_sessions_activation_code
        FOREIGN KEY (activation_code_id)
            REFERENCES activation_codes (id)
            ON DELETE CASCADE,

    CONSTRAINT fk_activation_sessions_result_credential
        FOREIGN KEY (result_device_credential_id)
            REFERENCES device_credentials (id)
            ON DELETE RESTRICT,

    CONSTRAINT chk_activation_sessions_result
        CHECK (
            (consumed_at IS NULL AND result_device_credential_id IS NULL)
            OR
            (consumed_at IS NOT NULL AND result_device_credential_id IS NOT NULL)
        ),

    CONSTRAINT chk_activation_sessions_terminal_state
        CHECK (consumed_at IS NULL OR invalidated_at IS NULL)
);

CREATE INDEX idx_activation_sessions_code
    ON activation_sessions (activation_code_id);

CREATE INDEX idx_activation_sessions_open
    ON activation_sessions (activation_code_id, expires_at)
    WHERE consumed_at IS NULL AND invalidated_at IS NULL;

CREATE UNIQUE INDEX idx_activation_sessions_one_open_per_code
    ON activation_sessions (activation_code_id)
    WHERE consumed_at IS NULL AND invalidated_at IS NULL;

CREATE TABLE auth_challenges
(
    id                   UUID PRIMARY KEY     DEFAULT gen_random_uuid(),
    device_credential_id UUID        NOT NULL,
    challenge            BYTEA       NOT NULL UNIQUE,
    expires_at           TIMESTAMPTZ NOT NULL,
    consumed_at          TIMESTAMPTZ,
    created_at           TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_auth_challenges_device_credential
        FOREIGN KEY (device_credential_id)
            REFERENCES device_credentials (id)
            ON DELETE CASCADE,

    CONSTRAINT chk_auth_challenges_expiry
        CHECK (expires_at > created_at),

    CONSTRAINT chk_auth_challenges_challenge_length
        CHECK (octet_length(challenge) = 32)
);

CREATE INDEX idx_auth_challenges_open
    ON auth_challenges (device_credential_id, expires_at) WHERE consumed_at IS NULL;

CREATE INDEX idx_auth_challenges_expires_at
    ON auth_challenges (expires_at);

CREATE UNIQUE INDEX idx_auth_challenges_one_open_per_credential
    ON auth_challenges (device_credential_id) WHERE consumed_at IS NULL;


CREATE TABLE nfc_tags
(
    id               UUID PRIMARY KEY      DEFAULT gen_random_uuid(),
    tag_uid          VARCHAR(255) NOT NULL UNIQUE,
    display_name     VARCHAR(255) NOT NULL,
    location         VARCHAR(255),
    room             VARCHAR(100),
    usage_type       VARCHAR(50)  NOT NULL DEFAULT 'BOTH',
    secret_reference VARCHAR(255),
    active           BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_nfc_tags_usage_type
        CHECK (usage_type IN ('CHECK_IN', 'CHECK_OUT', 'BOTH'))
);

CREATE INDEX idx_nfc_tags_room ON nfc_tags (room);
CREATE INDEX idx_nfc_tags_active ON nfc_tags (active);


CREATE TABLE lectures
(
    id                      UUID PRIMARY KEY      DEFAULT gen_random_uuid(),
    course_group_id         UUID         NOT NULL,
    lecturer_user_id        UUID,
    title                   VARCHAR(255) NOT NULL,
    room                    VARCHAR(100),
    starts_at               TIMESTAMPTZ  NOT NULL,
    ends_at                 TIMESTAMPTZ  NOT NULL,
    source                  VARCHAR(50)  NOT NULL DEFAULT 'MANUAL',
    external_reference      VARCHAR(255),
    active                  BOOLEAN      NOT NULL DEFAULT TRUE,
    trainex_source_key      VARCHAR(64),
    opened_at               TIMESTAMPTZ,
    protokollant_student_id UUID,
    attendance_validated_at TIMESTAMPTZ,
    attendance_validated_by UUID,
    created_at              TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at              TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_lectures_course_group
        FOREIGN KEY (course_group_id)
            REFERENCES course_groups (id)
            ON DELETE CASCADE,

    CONSTRAINT fk_lectures_lecturer_user
        FOREIGN KEY (lecturer_user_id)
            REFERENCES users (id)
            ON DELETE SET NULL,

    CONSTRAINT fk_lectures_protokollant
        FOREIGN KEY (protokollant_student_id)
            REFERENCES students (id)
            ON DELETE SET NULL,

    CONSTRAINT fk_lectures_attendance_validated_by
        FOREIGN KEY (attendance_validated_by)
            REFERENCES users (id)
            ON DELETE SET NULL,

    CONSTRAINT chk_lectures_source
        CHECK (source IN ('MANUAL', 'TRAINEX')),

    CONSTRAINT chk_lectures_time
        CHECK (ends_at > starts_at)
);

CREATE INDEX idx_lectures_course_group_id ON lectures (course_group_id);
CREATE INDEX idx_lectures_starts_at ON lectures (starts_at);
CREATE INDEX idx_lectures_ends_at ON lectures (ends_at);
CREATE INDEX idx_lectures_room ON lectures (room);
CREATE INDEX idx_lectures_active_time ON lectures (active, starts_at, ends_at);
CREATE UNIQUE INDEX uq_lectures_trainex_identity
    ON lectures (trainex_source_key, external_reference)
    WHERE source = 'TRAINEX' AND trainex_source_key IS NOT NULL AND external_reference IS NOT NULL;


CREATE TABLE attendance_events
(
    id                UUID PRIMARY KEY      DEFAULT gen_random_uuid(),
    student_id        UUID         NOT NULL,
    lecture_id        UUID,
    device_id         UUID         NOT NULL,
    nfc_tag_id        UUID         NOT NULL,
    event_type        VARCHAR(50)  NOT NULL,
    validation_status VARCHAR(50)  NOT NULL,
    nfc_payload_hash  VARCHAR(255) NOT NULL,
    occurred_at       TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    failure_reason    TEXT,
    created_at        TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_attendance_events_student
        FOREIGN KEY (student_id)
            REFERENCES students (id)
            ON DELETE CASCADE,

    CONSTRAINT fk_attendance_events_lecture
        FOREIGN KEY (lecture_id)
            REFERENCES lectures (id)
            ON DELETE SET NULL,

    CONSTRAINT fk_attendance_events_device
        FOREIGN KEY (device_id)
            REFERENCES devices (id)
            ON DELETE RESTRICT,

    CONSTRAINT fk_attendance_events_nfc_tag
        FOREIGN KEY (nfc_tag_id)
            REFERENCES nfc_tags (id)
            ON DELETE RESTRICT,

    CONSTRAINT chk_attendance_events_event_type
        CHECK (event_type IN ('CHECK_IN', 'CHECK_OUT')),

    CONSTRAINT chk_attendance_events_validation_status
        CHECK (validation_status IN
               (
                'VALID',
                'INVALID',
                'REPLAY_DETECTED',
                'DEVICE_NOT_APPROVED',
                'DEVICE_REVOKED',
                'INVALID_SIGNATURE',
                'NO_MATCHING_LECTURE'
                   ))
);

CREATE UNIQUE INDEX idx_attendance_events_valid_payload_hash
    ON attendance_events (nfc_payload_hash) WHERE validation_status = 'VALID';

CREATE INDEX idx_attendance_events_payload_hash
    ON attendance_events (nfc_payload_hash);

CREATE INDEX idx_attendance_events_student_id ON attendance_events (student_id);
CREATE INDEX idx_attendance_events_lecture_id ON attendance_events (lecture_id);
CREATE INDEX idx_attendance_events_occurred_at ON attendance_events (occurred_at);


CREATE TABLE attendance_records
(
    id                 UUID PRIMARY KEY     DEFAULT gen_random_uuid(),
    student_id         UUID        NOT NULL,
    lecture_id         UUID        NOT NULL,
    check_in_event_id  UUID,
    check_out_event_id UUID,
    check_in_time      TIMESTAMPTZ,
    check_out_time     TIMESTAMPTZ,
    check_out_source   VARCHAR(50) NOT NULL DEFAULT 'NONE',
    status             VARCHAR(50) NOT NULL DEFAULT 'ABSENT',
    manually_changed   BOOLEAN     NOT NULL DEFAULT FALSE,
    manual_note        TEXT,
    changed_by         UUID,
    created_at         TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at         TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_attendance_records_student
        FOREIGN KEY (student_id)
            REFERENCES students (id)
            ON DELETE CASCADE,

    CONSTRAINT fk_attendance_records_lecture
        FOREIGN KEY (lecture_id)
            REFERENCES lectures (id)
            ON DELETE CASCADE,

    CONSTRAINT fk_attendance_records_check_in_event
        FOREIGN KEY (check_in_event_id)
            REFERENCES attendance_events (id)
            ON DELETE SET NULL,

    CONSTRAINT fk_attendance_records_check_out_event
        FOREIGN KEY (check_out_event_id)
            REFERENCES attendance_events (id)
            ON DELETE SET NULL,

    CONSTRAINT fk_attendance_records_changed_by
        FOREIGN KEY (changed_by)
            REFERENCES users (id)
            ON DELETE SET NULL,

    CONSTRAINT chk_attendance_records_status
        CHECK (status IN ('PRESENT', 'LATE', 'LEFT_EARLY', 'ABSENT', 'SICK', 'EXCUSED', 'MANUAL_REVIEW')),

    CONSTRAINT chk_attendance_records_check_out_source
        CHECK (check_out_source IN ('NONE', 'AUTO', 'STUDENT', 'MANUAL')),

    CONSTRAINT uq_attendance_records_student_lecture
        UNIQUE (student_id, lecture_id)
);

CREATE INDEX idx_attendance_records_student_id ON attendance_records (student_id);
CREATE INDEX idx_attendance_records_lecture_id ON attendance_records (lecture_id);
CREATE INDEX idx_attendance_records_status ON attendance_records (status);


CREATE TABLE sick_notes
(
    id          UUID PRIMARY KEY     DEFAULT gen_random_uuid(),
    student_id  UUID        NOT NULL,
    starts_at   TIMESTAMPTZ NOT NULL,
    ends_at     TIMESTAMPTZ NOT NULL,
    reason      TEXT,
    status      VARCHAR(50) NOT NULL DEFAULT 'SUBMITTED',
    reviewed_by UUID,
    reviewed_at TIMESTAMPTZ,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_sick_notes_student
        FOREIGN KEY (student_id)
            REFERENCES students (id)
            ON DELETE CASCADE,

    CONSTRAINT fk_sick_notes_reviewed_by
        FOREIGN KEY (reviewed_by)
            REFERENCES users (id)
            ON DELETE SET NULL,

    CONSTRAINT chk_sick_notes_status
        CHECK (status IN ('SUBMITTED', 'APPROVED', 'REJECTED', 'WITHDRAWN')),

    CONSTRAINT chk_sick_notes_instant_range
        CHECK (ends_at > starts_at)
);

CREATE INDEX idx_sick_notes_student_id ON sick_notes (student_id);
CREATE INDEX idx_sick_notes_status ON sick_notes (status);
CREATE INDEX idx_sick_notes_instant_range ON sick_notes (starts_at, ends_at);


CREATE TABLE refresh_tokens
(
    id                   UUID PRIMARY KEY     DEFAULT gen_random_uuid(),
    family_id            UUID        NOT NULL,
    device_credential_id UUID        NOT NULL,
    token_hash           BYTEA       NOT NULL UNIQUE,
    session_version      BIGINT      NOT NULL,
    expires_at           TIMESTAMPTZ NOT NULL,
    family_expires_at    TIMESTAMPTZ NOT NULL,
    consumed_at          TIMESTAMPTZ,
    revoked_at           TIMESTAMPTZ,
    revocation_reason    VARCHAR(50),
    parent_token_id      UUID,
    replaced_by_token_id UUID,
    created_at           TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_refresh_tokens_device_credential
        FOREIGN KEY (device_credential_id)
            REFERENCES device_credentials (id)
            ON DELETE CASCADE,

    CONSTRAINT fk_refresh_tokens_parent
        FOREIGN KEY (parent_token_id)
            REFERENCES refresh_tokens (id)
            ON DELETE SET NULL,

    CONSTRAINT fk_refresh_tokens_replaced_by
        FOREIGN KEY (replaced_by_token_id)
            REFERENCES refresh_tokens (id)
            ON DELETE SET NULL,

    CONSTRAINT chk_refresh_tokens_expiry
        CHECK (expires_at > created_at),

    CONSTRAINT chk_refresh_tokens_hash_length
        CHECK (octet_length(token_hash) = 32),

    CONSTRAINT chk_refresh_tokens_session_version
        CHECK (session_version >= 0),

    CONSTRAINT chk_refresh_tokens_family_expiry
        CHECK (family_expires_at >= expires_at),

    CONSTRAINT chk_refresh_tokens_revocation_reason
        CHECK (revocation_reason IS NULL OR revocation_reason IN
               ('ROTATED', 'REPLAY_DETECTED', 'LOGOUT', 'DEVICE_REVOKED', 'DEVICE_REPLACED', 'NEW_LOGIN')),

    CONSTRAINT chk_refresh_tokens_revocation_state
        CHECK ((revoked_at IS NULL) = (revocation_reason IS NULL)),

    CONSTRAINT chk_refresh_tokens_rotation_state
        CHECK (replaced_by_token_id IS NULL OR consumed_at IS NOT NULL)
);

CREATE INDEX idx_refresh_tokens_family_id
    ON refresh_tokens (family_id);

CREATE INDEX idx_refresh_tokens_family_expires_at
    ON refresh_tokens (family_expires_at);

CREATE UNIQUE INDEX idx_refresh_tokens_one_active_per_credential
    ON refresh_tokens (device_credential_id) WHERE revoked_at IS NULL
      AND consumed_at IS NULL;

CREATE UNIQUE INDEX idx_refresh_tokens_one_child_per_parent
    ON refresh_tokens (parent_token_id)
    WHERE parent_token_id IS NOT NULL;

CREATE UNIQUE INDEX idx_refresh_tokens_unique_replacement
    ON refresh_tokens (replaced_by_token_id)
    WHERE replaced_by_token_id IS NOT NULL;

CREATE TABLE security_rate_limits
(
    rate_key          VARCHAR(255) PRIMARY KEY,
    window_started_at TIMESTAMPTZ NOT NULL,
    attempts          INTEGER     NOT NULL,
    blocked_until     TIMESTAMPTZ,
    updated_at        TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_security_rate_limits_attempts
        CHECK (attempts >= 0)
);

CREATE INDEX idx_security_rate_limits_updated_at
    ON security_rate_limits (updated_at);


CREATE TABLE audit_logs
(
    id              UUID PRIMARY KEY      DEFAULT gen_random_uuid(),
    actor_user_id   UUID,
    actor_device_id UUID,
    action          VARCHAR(100) NOT NULL,
    target_type     VARCHAR(100),
    target_id       UUID,
    ip_address      VARCHAR(100),
    user_agent      TEXT,
    details         TEXT,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_audit_logs_actor_user
        FOREIGN KEY (actor_user_id)
            REFERENCES users (id)
            ON DELETE SET NULL,

    CONSTRAINT fk_audit_logs_actor_device
        FOREIGN KEY (actor_device_id)
            REFERENCES devices (id)
            ON DELETE SET NULL
);

CREATE INDEX idx_audit_logs_actor_user_id ON audit_logs (actor_user_id);
CREATE INDEX idx_audit_logs_actor_device_id ON audit_logs (actor_device_id);
CREATE INDEX idx_audit_logs_action ON audit_logs (action);
CREATE INDEX idx_audit_logs_target ON audit_logs (target_type, target_id);
CREATE INDEX idx_audit_logs_created_at ON audit_logs (created_at);
