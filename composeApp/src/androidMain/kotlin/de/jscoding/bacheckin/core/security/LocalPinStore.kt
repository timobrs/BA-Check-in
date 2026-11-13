package de.jscoding.bacheckin.core.security

import android.content.Context
import android.util.Base64
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import java.security.MessageDigest
import java.security.SecureRandom
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
/**
 * Speichert die vom Nutzer gewählte lokale PIN als gehashte und gesalte Referenz.
 */
class LocalPinStore @Inject constructor(
    @ApplicationContext context: Context
) {
    companion object {
        private const val PREFS_NAME = "local_pin_store"
        private const val KEY_PIN_HASH = "pin_hash"
        private const val KEY_PIN_SALT = "pin_salt"
        private const val SALT_SIZE_BYTES = 16
        private const val PIN_LENGTH = 5
    }

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val secureRandom = SecureRandom()

    fun save(pin: String) {
        require(isValidPin(pin)) { "PIN muss aus 5 Ziffern bestehen." }

        val salt = ByteArray(SALT_SIZE_BYTES).also(secureRandom::nextBytes)
        prefs.edit {
            putString(KEY_PIN_HASH, hashPin(pin, salt))
            putString(KEY_PIN_SALT, Base64.encodeToString(salt, Base64.NO_WRAP))
        }
    }

    fun matches(pin: String): Boolean {
        if (!isValidPin(pin)) return false

        val storedHashBase64 = prefs.getString(KEY_PIN_HASH, null) ?: return false
        val storedSaltBase64 = prefs.getString(KEY_PIN_SALT, null) ?: return false
        val salt = Base64.decode(storedSaltBase64, Base64.NO_WRAP)
        val expectedHash = Base64.decode(storedHashBase64, Base64.NO_WRAP)
        val actualHash = Base64.decode(hashPin(pin, salt), Base64.NO_WRAP)
        return MessageDigest.isEqual(expectedHash, actualHash)
    }

    fun clear() {
        prefs.edit { clear() }
    }

    private fun hashPin(pin: String, salt: ByteArray): String {
        val digest = MessageDigest.getInstance("SHA-256")
        digest.update(salt)
        digest.update(pin.toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(digest.digest(), Base64.NO_WRAP)
    }

    private fun isValidPin(pin: String): Boolean =
        pin.length == PIN_LENGTH && pin.all(Char::isDigit)
}
