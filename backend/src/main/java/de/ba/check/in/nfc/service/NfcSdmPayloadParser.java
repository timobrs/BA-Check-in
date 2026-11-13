package de.ba.check.in.nfc.service;

import java.util.HexFormat;

public final class NfcSdmPayloadParser {

    /** Erstellt eine neue Instanz. */
    private NfcSdmPayloadParser() {}

    /** Liest die NFC-Daten. */
    public static ParsedPayload parse(String encryptedPiccDataHex, String sdmMacHex) {
        return new ParsedPayload(
                parseHex(encryptedPiccDataHex, 16, "Encrypted PICCData"),
                parseHex(sdmMacHex, 8, "SDM MAC"));
    }

    /** Liest einen Hexadezimalwert. */
    private static byte[] parseHex(String value, int expectedBytes, String name) {
        if (value == null || !value.matches("(?i)[0-9a-f]{" + (expectedBytes * 2) + "}")) {
            throw new IllegalArgumentException(name + " must contain exactly " + expectedBytes * 2 + " hex characters");
        }
        return HexFormat.of().parseHex(value);
    }

    public record ParsedPayload(byte[] encryptedPiccData, byte[] sdmMac) {}
}
