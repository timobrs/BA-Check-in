ALTER TABLE nfc_tags
    ADD COLUMN last_read_counter BIGINT NOT NULL DEFAULT -1;

ALTER TABLE nfc_tags
    ADD CONSTRAINT chk_nfc_tags_read_counter
        CHECK (last_read_counter BETWEEN -1 AND 16777215);
