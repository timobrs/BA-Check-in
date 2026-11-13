package de.ba.check.in.nfc.service;

import de.ba.check.in.nfc.domain.NFCTagEntity;

public interface NfcKeyProvider {

    /** Lädt die passenden Schlüssel. */
    NfcCardKeys keysFor(NFCTagEntity tag);
}
