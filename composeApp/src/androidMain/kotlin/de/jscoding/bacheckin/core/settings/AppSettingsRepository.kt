package de.jscoding.bacheckin.core.settings

import kotlinx.coroutines.flow.Flow

interface AppSettingsRepository {
    fun observeSettings(): Flow<AppSettings>
    suspend fun setThemePreference(themePreference: ThemePreference)
    suspend fun setBiometricPreferred(enabled: Boolean)
}
