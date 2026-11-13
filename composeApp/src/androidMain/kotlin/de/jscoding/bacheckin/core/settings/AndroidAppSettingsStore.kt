package de.jscoding.bacheckin.core.settings

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.appSettingsDataStore by preferencesDataStore(
    name = "app_settings"
)

@Singleton
class AndroidAppSettingsStore @Inject constructor(
    @param:ApplicationContext private val context: Context
) : AppSettingsRepository {
    private object Keys {
        val THEME_PREFERENCE = stringPreferencesKey("theme_preference")
        val BIOMETRIC_PREFERRED = booleanPreferencesKey("biometric_preferred")
    }

    override fun observeSettings(): Flow<AppSettings> =
        context.appSettingsDataStore.data.map { prefs ->
            AppSettings(
                themePreference = prefs[Keys.THEME_PREFERENCE]
                    ?.let { runCatching { ThemePreference.valueOf(it) }.getOrNull() }
                    ?: ThemePreference.SYSTEM,
                biometricPreferred = prefs[Keys.BIOMETRIC_PREFERRED] ?: false,
            )
        }

    override suspend fun setThemePreference(themePreference: ThemePreference) {
        context.appSettingsDataStore.edit { prefs ->
            prefs[Keys.THEME_PREFERENCE] = themePreference.name
        }
    }

    override suspend fun setBiometricPreferred(enabled: Boolean) {
        context.appSettingsDataStore.edit { prefs ->
            prefs[Keys.BIOMETRIC_PREFERRED] = enabled
        }
    }
}
