package de.jscoding.bacheckin.core.device

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import de.jscoding.bacheckin.core.security.DeviceKeyManager
import de.jscoding.bacheckin.shared.model.UserRole
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.deviceBindingDataStore by preferencesDataStore(
    name = "device_binding"
)

@Singleton
class AndroidDeviceStateStore @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val deviceKeyManager: DeviceKeyManager
) {
    private object Keys {
        val STATUS = stringPreferencesKey("status")
        val USER_ID = stringPreferencesKey("user_id")
        val DISPLAY_NAME = stringPreferencesKey("display_name")
        val DEVICE_CREDENTIAL_ID = stringPreferencesKey("device_credential_id")
        val ACCESS_TOKEN_EXPIRES_AT = longPreferencesKey("access_token_expires_at")
    }

    val state: Flow<DeviceBindingState> =
        context.deviceBindingDataStore.data.map { prefs ->
            DeviceBindingState(
                status = prefs[Keys.STATUS]
                    ?.let { runCatching { DeviceBindingStatus.valueOf(it) }.getOrNull() }
                    ?: DeviceBindingStatus.UNACTIVATED,
                userId = prefs[Keys.USER_ID],
                userRole = deviceKeyManager.getUserRole(),
                displayName = prefs[Keys.DISPLAY_NAME],
                deviceCredentialId = prefs[Keys.DEVICE_CREDENTIAL_ID],
                accessTokenExpiresAtEpochMillis = prefs[Keys.ACCESS_TOKEN_EXPIRES_AT]
            )
        }

    suspend fun setActivated(
        userId: String,
        userRole: UserRole,
        displayName: String?,
        deviceCredentialId: String,
        accessTokenExpiresAtEpochMillis: Long
    ) {
        deviceKeyManager.saveUserRole(userRole)
        context.deviceBindingDataStore.edit { prefs ->
            prefs[Keys.STATUS] = DeviceBindingStatus.ACTIVATED.name
            prefs[Keys.USER_ID] = userId
            displayName?.let { prefs[Keys.DISPLAY_NAME] = it } ?: prefs.remove(Keys.DISPLAY_NAME)
            prefs[Keys.DEVICE_CREDENTIAL_ID] = deviceCredentialId
            prefs[Keys.ACCESS_TOKEN_EXPIRES_AT] = accessTokenExpiresAtEpochMillis
        }
    }

    suspend fun updateTokenExpiry(accessTokenExpiresAtEpochMillis: Long) {
        context.deviceBindingDataStore.edit { prefs ->
            prefs[Keys.ACCESS_TOKEN_EXPIRES_AT] = accessTokenExpiresAtEpochMillis
        }
    }

    suspend fun setRevoked() {
        context.deviceBindingDataStore.edit { prefs ->
            prefs[Keys.STATUS] = DeviceBindingStatus.REVOKED.name
            prefs.remove(Keys.ACCESS_TOKEN_EXPIRES_AT)
        }
    }

    suspend fun clear() {
        deviceKeyManager.deleteUserRole()
        context.deviceBindingDataStore.edit { it.clear() }
    }
}
