package de.jscoding.bacheckin.core.session

import android.content.Context
import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import javax.inject.Singleton

data class StoredTokens(
    val accessToken: String,
    val refreshToken: String?,
    val accessTokenExpiresAtEpochMillis: Long
)

/** Tokens werden mit einem Keystore-Schlüssel verschlüsselt gespeichert. */
@Singleton
class SecureTokenStore @Inject constructor(
    @ApplicationContext context: Context
) {
    companion object {
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val TOKEN_KEY_ALIAS = "bacheckin_token_aes_key_v1"
        private const val PREFS_NAME = "secure_token_ciphertext"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val KEY_ACCESS_TOKEN = "access_token"
        private const val KEY_ACCESS_IV = "access_iv"
        private const val KEY_REFRESH_TOKEN = "refresh_token"
        private const val KEY_REFRESH_IV = "refresh_iv"
        private const val KEY_EXPIRES_AT = "expires_at"
    }

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val keyStore: KeyStore =
        KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }

    fun save(tokens: StoredTokens) {
        val encryptedAccessToken = encrypt(tokens.accessToken)
        prefs.edit {
            putString(KEY_ACCESS_TOKEN, encryptedAccessToken.ciphertextBase64)
            putString(KEY_ACCESS_IV, encryptedAccessToken.ivBase64)
            putLong(KEY_EXPIRES_AT, tokens.accessTokenExpiresAtEpochMillis)

            if (tokens.refreshToken == null) {
                remove(KEY_REFRESH_TOKEN)
                remove(KEY_REFRESH_IV)
            } else {
                val encryptedRefreshToken = encrypt(tokens.refreshToken)
                putString(KEY_REFRESH_TOKEN, encryptedRefreshToken.ciphertextBase64)
                putString(KEY_REFRESH_IV, encryptedRefreshToken.ivBase64)
            }
        }
    }

    fun read(): StoredTokens? = runCatching {
        val accessCiphertext = prefs.getString(KEY_ACCESS_TOKEN, null) ?: return null
        val accessIv = prefs.getString(KEY_ACCESS_IV, null) ?: return null
        val refreshCiphertext = prefs.getString(KEY_REFRESH_TOKEN, null)
        val refreshIv = prefs.getString(KEY_REFRESH_IV, null)

        StoredTokens(
            accessToken = decrypt(accessCiphertext, accessIv),
            refreshToken = if (refreshCiphertext != null && refreshIv != null) {
                decrypt(refreshCiphertext, refreshIv)
            } else {
                null
            },
            accessTokenExpiresAtEpochMillis = prefs.getLong(KEY_EXPIRES_AT, 0L)
        )
    }.getOrElse {
        clear()
        null
    }

    fun clear() {
        prefs.edit { clear() }
    }

    private fun encrypt(plainText: String): EncryptedValue {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateSecretKey())
        val ciphertext = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
        return EncryptedValue(
            ciphertextBase64 = Base64.encodeToString(ciphertext, Base64.NO_WRAP),
            ivBase64 = Base64.encodeToString(cipher.iv, Base64.NO_WRAP)
        )
    }

    private fun decrypt(ciphertextBase64: String, ivBase64: String): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(
            Cipher.DECRYPT_MODE,
            getOrCreateSecretKey(),
            GCMParameterSpec(128, Base64.decode(ivBase64, Base64.NO_WRAP))
        )
        val plaintext = cipher.doFinal(Base64.decode(ciphertextBase64, Base64.NO_WRAP))
        return plaintext.toString(Charsets.UTF_8)
    }

    private fun getOrCreateSecretKey(): SecretKey {
        val existing = keyStore.getKey(TOKEN_KEY_ALIAS, null) as? SecretKey
        if (existing != null) return existing

        val generator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            ANDROID_KEYSTORE
        )
        val spec = KeyGenParameterSpec.Builder(
            TOKEN_KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setRandomizedEncryptionRequired(true)
            .build()

        generator.init(spec)
        return generator.generateKey()
    }

    private data class EncryptedValue(
        val ciphertextBase64: String,
        val ivBase64: String
    )
}
