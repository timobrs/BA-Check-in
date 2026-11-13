package de.ba.check.in.nfc.service;

public record NfcCardKeys(byte[] metadataReadKey, byte[] fileReadKey) {

    /** Erstellt eine neue Instanz. */
    public NfcCardKeys {
        metadataReadKey = metadataReadKey.clone();
        fileReadKey = fileReadKey.clone();
    }
}
