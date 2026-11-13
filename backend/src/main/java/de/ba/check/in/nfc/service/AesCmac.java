package de.ba.check.in.nfc.service;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.security.GeneralSecurityException;
import java.util.Arrays;

final class AesCmac {

    /** Erstellt eine neue Instanz. */
    private AesCmac() {}

    /** Berechnet den CMAC. */
    static byte[] calculate(byte[] key, byte[] message) {
        byte[] firstSubKey = nextSubKey(encryptBlock(key, new byte[16]));
        byte[] secondSubKey = nextSubKey(firstSubKey);
        int blockCount = Math.max(1, (message.length + 15) / 16);
        boolean complete = message.length > 0 && message.length % 16 == 0;
        byte[] finalBlock = new byte[16];
        int offset = (blockCount - 1) * 16;
        if (complete) {
            System.arraycopy(message, offset, finalBlock, 0, 16);
            xor(finalBlock, firstSubKey);
        } else {
            int remaining = message.length - offset;
            if (remaining > 0) System.arraycopy(message, offset, finalBlock, 0, remaining);
            finalBlock[remaining] = (byte) 0x80;
            xor(finalBlock, secondSubKey);
        }
        byte[] state = new byte[16];
        for (int index = 0; index < blockCount - 1; index++) {
            byte[] block = Arrays.copyOfRange(message, index * 16, index * 16 + 16);
            xor(block, state);
            state = encryptBlock(key, block);
        }
        xor(finalBlock, state);
        return encryptBlock(key, finalBlock);
    }

    /** Verschlüsselt einen Datenblock. */
    private static byte[] encryptBlock(byte[] key, byte[] block) {
        try {
            Cipher cipher = Cipher.getInstance("AES/ECB/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(key, "AES"));
            return cipher.doFinal(block);
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("AES-CMAC could not be calculated", exception);
        }
    }

    /** Berechnet den nächsten Teilschlüssel. */
    private static byte[] nextSubKey(byte[] previous) {
        byte[] result = new byte[16];
        int carry = 0;
        for (int index = 15; index >= 0; index--) {
            int value = previous[index] & 0xff;
            result[index] = (byte) ((value << 1) | carry);
            carry = value >>> 7;
        }
        if (carry != 0) result[15] ^= (byte) 0x87;
        return result;
    }

    /** Verknüpft die Bytes mit XOR. */
    private static void xor(byte[] target, byte[] other) {
        for (int index = 0; index < target.length; index++) target[index] ^= other[index];
    }
}
