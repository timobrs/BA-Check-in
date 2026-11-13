package de.jscoding.bacheckin.core.security

import android.content.Context
import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.security.keystore.StrongBoxUnavailableException
import android.util.Base64
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import de.jscoding.bacheckin.shared.model.UserRole
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.PrivateKey
import java.security.Signature
import java.security.spec.ECGenParameterSpec
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton

data class DeviceKeyMaterial(
    val publicKeyBase64: String
)

@Singleton
class DeviceKeyManager @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val deviceSecurityChecker: DeviceSecurityChecker
) {
    companion object {
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val DEVICE_KEY_ALIAS = "bacheckin_device_identity_key_v1"
        private const val ROLE_KEY_ALIAS = "bacheckin_role_aes_key_v1"
        private const val SIGNATURE_ALGORITHM = "SHA256withECDSA"
        private const val PREFS_NAME = "secure_role_storage"
        private const val KEY_USER_ROLE = "encrypted_user_role"
        private const val KEY_ROLE_IV = "role_iv"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
    }

    private val keyStore: KeyStore =
        KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun hasDeviceKey(): Boolean = keyStore.containsAlias(DEVICE_KEY_ALIAS)

    fun deleteDeviceKey() {
        if (keyStore.containsAlias(DEVICE_KEY_ALIAS)) {
            keyStore.deleteEntry(DEVICE_KEY_ALIAS)
        }
    }

    fun canUseBiometricAuth(): Boolean = deviceSecurityChecker.canUseBiometricAuthentication()

    fun saveUserRole(role: UserRole) {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateRoleSecretKey())
        val ciphertext = cipher.doFinal(role.name.toByteArray(Charsets.UTF_8))
        
        prefs.edit {
            putString(KEY_USER_ROLE, Base64.encodeToString(ciphertext, Base64.NO_WRAP))
            putString(KEY_ROLE_IV, Base64.encodeToString(cipher.iv, Base64.NO_WRAP))
        }
    }

    fun getUserRole(): UserRole {
        val ciphertextBase64 = prefs.getString(KEY_USER_ROLE, null) ?: return UserRole.NONE
        val ivBase64 = prefs.getString(KEY_ROLE_IV, null) ?: return UserRole.NONE

        return runCatching {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(
                Cipher.DECRYPT_MODE,
                getOrCreateRoleSecretKey(),
                GCMParameterSpec(128, Base64.decode(ivBase64, Base64.NO_WRAP))
            )
            val plaintext = cipher.doFinal(Base64.decode(ciphertextBase64, Base64.NO_WRAP))
            UserRole.valueOf(plaintext.toString(Charsets.UTF_8))
        }.getOrElse { UserRole.NONE }
    }

    fun deleteUserRole() {
        prefs.edit { clear() }
        if (keyStore.containsAlias(ROLE_KEY_ALIAS)) {
            keyStore.deleteEntry(ROLE_KEY_ALIAS)
        }
    }

    private fun getOrCreateRoleSecretKey(): SecretKey {
        val existing = keyStore.getKey(ROLE_KEY_ALIAS, null) as? SecretKey
        if (existing != null) return existing

        val generator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            ANDROID_KEYSTORE
        )
        val spec = KeyGenParameterSpec.Builder(
            ROLE_KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setRandomizedEncryptionRequired(true)
            .build()

        generator.init(spec)
        return generator.generateKey()
    }

    fun generateDeviceKey(preferStrongBox: Boolean = true): DeviceKeyMaterial {
        deleteDeviceKey()

        return if (preferStrongBox && Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            runCatching {
                generateInternal(useStrongBox = true)
            }.recoverCatching { error ->
                if (error is StrongBoxUnavailableException) {
                    generateInternal(useStrongBox = false)
                } else {
                    throw error
                }
            }.getOrThrow()
        } else {
            generateInternal(useStrongBox = false)
        }
    }

    fun signPayloadBase64(payload: ByteArray): String {
        val signature = Signature.getInstance(SIGNATURE_ALGORITHM)
        signature.initSign(getPrivateKey())
        signature.update(payload)
        return Base64.encodeToString(signature.sign(), Base64.NO_WRAP)
    }

    private fun generateInternal(useStrongBox: Boolean): DeviceKeyMaterial {
        val generator = KeyPairGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_EC,
            ANDROID_KEYSTORE
        )

        val specBuilder = KeyGenParameterSpec.Builder(
            DEVICE_KEY_ALIAS,
            KeyProperties.PURPOSE_SIGN or KeyProperties.PURPOSE_VERIFY
        )
            .setAlgorithmParameterSpec(ECGenParameterSpec("secp256r1"))
            .setDigests(KeyProperties.DIGEST_SHA256)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && useStrongBox) {
            specBuilder.setIsStrongBoxBacked(true)
        }

        generator.initialize(specBuilder.build())
        val keyPair = generator.generateKeyPair()

        return DeviceKeyMaterial(
            publicKeyBase64 = Base64.encodeToString(keyPair.public.encoded, Base64.NO_WRAP)
        )
    }

    private fun getPrivateKey(): PrivateKey {
        return keyStore.getKey(DEVICE_KEY_ALIAS, null) as? PrivateKey
            ?: error("Device private key missing")
    }

}
