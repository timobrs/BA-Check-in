package de.ba.check.in.auth.devicekey.service;

import de.ba.check.in.auth.activation.dto.CompleteActivationRequest;
import de.ba.check.in.auth.crypto.DeviceSignatureService;
import de.ba.check.in.auth.crypto.Sha256Hasher;
import de.ba.check.in.auth.devicekey.exception.DeviceKeyRejectedException;
import de.ba.check.in.auth.devicekey.model.VerifiedDeviceKey;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.security.AlgorithmParameters;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.ECParameterSpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

@Service
@RequiredArgsConstructor
public class DeviceKeyProofVerifier {
    private final DeviceSignatureService signatureService;
    private final Sha256Hasher sha256Hasher;

    /** Prüft den Gerätezugangsnachweis. */
    public VerifiedDeviceKey verify(CompleteActivationRequest request, byte[] challenge) {
        try {
            byte[] encodedKey = Base64.getDecoder().decode(request.publicKeyBase64());
            PublicKey publicKey = KeyFactory.getInstance("EC").generatePublic(new X509EncodedKeySpec(encodedKey));
            if (!(publicKey instanceof ECPublicKey ecPublicKey) || !usesP256(ecPublicKey)) {
                throw new DeviceKeyRejectedException("Only P-256 device keys are accepted");
            }
            byte[] normalizedKey = publicKey.getEncoded();
            byte[] fingerprint = sha256Hasher.hash(normalizedKey);
            byte[] proof = DeviceActivationProof.payload(request.activationSessionId(), challenge, fingerprint);
            if (!signatureService.verify(Base64.getEncoder().encodeToString(normalizedKey),
                    proof,
                    request.challengeSignatureBase64())) {
                throw new DeviceKeyRejectedException("Device key possession proof is invalid");
            }
            return new VerifiedDeviceKey(Base64.getEncoder().encodeToString(normalizedKey), fingerprint, "ES256");
        } catch (DeviceKeyRejectedException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new DeviceKeyRejectedException("Device public key is invalid");
        }
    }

    /** Prüft die Verwendung von P-256. */
    private boolean usesP256(ECPublicKey publicKey) throws Exception {
        AlgorithmParameters parameters = AlgorithmParameters.getInstance("EC");
        parameters.init(new ECGenParameterSpec("secp256r1"));
        ECParameterSpec expected = parameters.getParameterSpec(ECParameterSpec.class);
        ECParameterSpec actual = publicKey.getParams();
        return expected.getCurve().equals(actual.getCurve())
                && expected.getGenerator().equals(actual.getGenerator())
                && expected.getOrder().equals(actual.getOrder())
                && expected.getCofactor() == actual.getCofactor();
    }
}
