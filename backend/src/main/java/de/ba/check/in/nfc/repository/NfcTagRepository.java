package de.ba.check.in.nfc.repository;

import de.ba.check.in.nfc.domain.NFCTagEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;
import java.util.Optional;

public interface NfcTagRepository extends JpaRepository<NFCTagEntity, UUID> {

    /** Prüft, ob passende NFC-Karten existieren. */
    boolean existsByTagUidIgnoreCase(String tagUid);

    /** Sucht passende NFC-Karten. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select tag from NFCTagEntity tag where lower(tag.tagUid) = lower(:uid)")
    Optional<NFCTagEntity> findByUidForUpdate(@Param("uid") String uid);

    /** Sucht passende NFC-Karten. */
    Optional<NFCTagEntity> findByTagUidIgnoreCaseAndActiveTrue(String tagUid);

    /** Sucht passende NFC-Karten. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select tag from NFCTagEntity tag where lower(tag.tagUid) = lower(:tagUid) and tag.active = true")
    Optional<NFCTagEntity> findActiveByUidForUpdate(@Param("tagUid") String tagUid);

    /** Sucht passende NFC-Karten. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select tag from NFCTagEntity tag where tag.publicId = :publicId")
    Optional<NFCTagEntity> findByPublicIdForUpdate(@Param("publicId") UUID publicId);
}
