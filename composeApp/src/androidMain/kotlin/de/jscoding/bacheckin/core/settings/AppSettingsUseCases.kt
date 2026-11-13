package de.jscoding.bacheckin.core.settings

class ObserveAppSettingsUseCase(
    private val repository: AppSettingsRepository
) {
    operator fun invoke() = repository.observeSettings()
}

class SetThemePreferenceUseCase(
    private val repository: AppSettingsRepository
) {
    suspend operator fun invoke(themePreference: ThemePreference) =
        repository.setThemePreference(themePreference)
}

class SetBiometricPreferenceUseCase(
    private val repository: AppSettingsRepository
) {
    suspend operator fun invoke(enabled: Boolean) =
        repository.setBiometricPreferred(enabled)
}
