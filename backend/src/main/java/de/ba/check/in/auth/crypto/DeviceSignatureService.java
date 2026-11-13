package de.ba.check.in.auth.crypto;

import org.springframework.stereotype.Service;

import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

@Service
public class DeviceSignatureService {

    /** Prüft die Gerätesignatur. */
    public boolean verify(String publicKeyBase64, byte[] payload, String signatureBase64) {
        try {
            byte[] encodedPublicKey = Base64.getDecoder().decode(publicKeyBase64);
            X509EncodedKeySpec keySpec = new X509EncodedKeySpec(encodedPublicKey);
            PublicKey publicKey = KeyFactory.getInstance("EC").generatePublic(keySpec);
            Signature verifier = Signature.getInstance("SHA256withECDSA");
            verifier.initVerify(publicKey);
            verifier.update(payload);
            byte[] signature = decodeBase64(signatureBase64);
            return verifier.verify(signature);
        } catch (Exception error) {
            return false;
        }
    }

    /** Dekodiert den Base64-Wert. */
    private byte[] decodeBase64(String value) {
        try {
            return Base64.getDecoder().decode(value);
        } catch (IllegalArgumentException ignored) {
            return Base64.getUrlDecoder().decode(value);
        }
    }
}
