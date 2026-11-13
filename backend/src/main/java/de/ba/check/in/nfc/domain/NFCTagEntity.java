package de.ba.check.in.nfc.domain;

import de.ba.check.in.common.persistence.TimestampedEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "nfc_tags")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class NFCTagEntity extends TimestampedEntity {

    @Column(name = "public_id", nullable = false, unique = true, updatable = false)
    private UUID publicId = UUID.randomUUID();

    @Column(name = "tag_uid", nullable = false, unique = true, length = 255)
    private String tagUid;

    @Column(name = "display_name", nullable = false, length = 255)
    private String displayName;

    @Column(name = "location", length = 255)
    private String location;

    @Column(name = "room", length = 100)
    private String room;

    @Column(name = "secret_reference", length = 255)
    private String secretReference;

    @Column(name = "key_version", nullable = false, length = 50)
    private String keyVersion = "v1";

    @Column(name = "active", nullable = false)
    private boolean active = true;

    @Column(name = "last_read_counter", nullable = false)
    private long lastReadCounter = -1;

    @Column(name = "last_seen_at")
    private Instant lastSeenAt;

    /** Erstellt eine NFC-Karte. */
    public static NFCTagEntity create(String uid, String name, String version) {
        NFCTagEntity tag = new NFCTagEntity();
        tag.tagUid = uid;
        tag.displayName = name;
        tag.keyVersion = version;
        return tag;
    }

    /** Speichert den Lesezähler. */
    public void recordReadCounter(long readCounter, Instant seenAt) {
        this.lastReadCounter = readCounter;
        this.lastSeenAt = seenAt;
    }
}
