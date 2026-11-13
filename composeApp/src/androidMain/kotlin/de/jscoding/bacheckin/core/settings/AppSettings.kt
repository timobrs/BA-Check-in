package de.jscoding.bacheckin.core.settings

enum class ThemePreference {
    SYSTEM,
    LIGHT,
    DARK
}

data class AppSettings(
    val themePreference: ThemePreference = ThemePreference.SYSTEM,
    val biometricPreferred: Boolean = false
)
