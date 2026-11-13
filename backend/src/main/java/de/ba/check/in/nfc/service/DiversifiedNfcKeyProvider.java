package de.ba.check.in.nfc.service;

import de.ba.check.in.nfc.domain.NFCTagEntity;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HexFormat;
import java.security.MessageDigest;

@Component
public class DiversifiedNfcKeyProvider implements NfcKeyProvider {
    private final byte[] metadataMasterKey;
    private final byte[] authenticationMasterKey;
    private final String configuredVersion;

    /** Erstellt eine neue Instanz. */
    public DiversifiedNfcKeyProvider(
            @Value("${app.nfc.key-version:v1}") String configuredVersion,
            @Value("${app.nfc.metadata-master-key-file:}") String metadataKeyFile,
            @Value("${app.nfc.authentication-master-key-file:}") String authenticationKeyFile,
            @Value("${app.nfc.metadata-master-key-hex:}") String metadataKeyHex,
            @Value("${app.nfc.authentication-master-key-hex:}") String authenticationKeyHex,
            @Value("${app.nfc.allow-insecure-keys:false}") boolean allowInsecureKeys) {
        this.configuredVersion = configuredVersion;
        this.metadataMasterKey = loadKey(metadataKeyFile, metadataKeyHex, "metadata master key");
        this.authenticationMasterKey = loadKey(authenticationKeyFile, authenticationKeyHex, "authentication master key");
        if (!allowInsecureKeys && (allZero(metadataMasterKey) || allZero(authenticationMasterKey)
                || MessageDigest.isEqual(metadataMasterKey, authenticationMasterKey))) {
            throw new IllegalStateException("Default, null, or identical NFC master keys are forbidden outside development");
        }
    }

    /** Lädt die passenden Schlüssel. */
    @Override
    public NfcCardKeys keysFor(NFCTagEntity tag) {
        if (!configuredVersion.equals(tag.getKeyVersion())) {
            throw new IllegalArgumentException("Unsupported NFC key version");
        }
        byte[] tagId = parseHex(tag.getTagUid(), 7, "tag UID");
        return new NfcCardKeys(
                diversify(metadataMasterKey, tagId, tag.getKeyVersion(), (byte) 0x01),
                diversify(authenticationMasterKey, tagId, tag.getKeyVersion(), (byte) 0x02));
    }

    /** Leitet einen kartenspezifischen Schlüssel ab. */
    static byte[] diversify(byte[] masterKey, byte[] tagId, String version, byte purpose) {
        byte[] versionBytes = version.getBytes(StandardCharsets.UTF_8);
        if (versionBytes.length > 16) {
            throw new IllegalArgumentException("NFC key version is too long");
        }
        byte[] diversificationData = new byte[32];
        diversificationData[0] = 0x01;
        diversificationData[1] = purpose;
        diversificationData[2] = (byte) tagId.length;
        System.arraycopy(tagId, 0, diversificationData, 3, tagId.length);
        diversificationData[10] = (byte) versionBytes.length;
        System.arraycopy(versionBytes, 0, diversificationData, 11, versionBytes.length);
        diversificationData[11 + versionBytes.length] = (byte) 0x80;
        return AesCmac.calculate(masterKey, diversificationData);
    }

    /** Lädt den Schlüssel. */
    private static byte[] loadKey(String file, String inlineHex, String name) {
        String value = inlineHex == null ? "" : inlineHex.trim();
        if (file != null && !file.isBlank()) {
            try {
                value = Files.readString(Path.of(file.trim())).trim();
            } catch (IOException exception) {
                throw new IllegalStateException("Could not read NFC " + name + " file", exception);
            }
        }
        return parseHex(value, 16, name);
    }

    /** Liest einen Hexadezimalwert. */
    private static byte[] parseHex(String value, int expectedBytes, String name) {
        if (value == null || !value.matches("(?i)[0-9a-f]{" + expectedBytes * 2 + "}")) {
            throw new IllegalStateException(
                    "NFC " + name + " must contain exactly "
                            + expectedBytes * 2 + " hex characters");
        }
        return HexFormat.of().parseHex(value);
    }

    /** Prüft, ob alle Bytes null sind. */
    private static boolean allZero(byte[] value) {
        int combined = 0;
        for (byte item : value) {
            combined |= item;
        }
        return combined == 0;
    }
}
