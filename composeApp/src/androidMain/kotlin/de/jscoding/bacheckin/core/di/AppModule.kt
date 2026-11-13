package de.jscoding.bacheckin.core.di

import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import de.jscoding.bacheckin.BuildConfig
import de.jscoding.bacheckin.core.device.AndroidAppVersionProvider
import de.jscoding.bacheckin.core.device.AndroidDeviceAuthRepository
import de.jscoding.bacheckin.core.network.AndroidNetworkMonitor
import de.jscoding.bacheckin.core.network.BackendRemoteDataSourceAuthWrapper
import de.jscoding.bacheckin.core.security.DeviceSecurityChecker
import de.jscoding.bacheckin.core.settings.AndroidAppSettingsStore
import de.jscoding.bacheckin.core.device.ActivateDeviceUseCase
import de.jscoding.bacheckin.core.device.AppVersionProvider
import de.jscoding.bacheckin.core.device.DeviceAuthRepository
import de.jscoding.bacheckin.core.device.ObserveDeviceBindingUseCase
import de.jscoding.bacheckin.core.device.ObserveDeviceSecurityUseCase
import de.jscoding.bacheckin.core.device.OpenDeviceSessionUseCase
import de.jscoding.bacheckin.core.device.RevokeLocalDeviceUseCase
import de.jscoding.bacheckin.core.device.ValidateActivationCodeUseCase
import de.jscoding.bacheckin.shared.model.BackendMode
import de.jscoding.bacheckin.shared.model.BackendRemoteDataSource
import de.jscoding.bacheckin.shared.dataSource.BackendRemoteDataSourceImpl
import de.jscoding.bacheckin.shared.model.BackendRuntimeConfig
import de.jscoding.bacheckin.core.network.NetworkMonitor
import de.jscoding.bacheckin.shared.dataSource.PostgresBackendRemoteDataSource
import de.jscoding.bacheckin.core.settings.AppSettingsRepository
import de.jscoding.bacheckin.core.settings.ObserveAppSettingsUseCase
import de.jscoding.bacheckin.core.settings.SetBiometricPreferenceUseCase
import de.jscoding.bacheckin.core.settings.SetThemePreferenceUseCase
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AppModule {

    @Binds
    @Singleton
    abstract fun bindNetworkMonitor(
        implementation: AndroidNetworkMonitor
    ): NetworkMonitor

    @Binds
    @Singleton
    abstract fun bindDeviceAuthRepository(
        implementation: AndroidDeviceAuthRepository
    ): DeviceAuthRepository

    @Binds
    @Singleton
    abstract fun bindBackendRemoteDataSource(
        implementation: BackendRemoteDataSourceAuthWrapper
    ): BackendRemoteDataSource

    @Binds
    @Singleton
    abstract fun bindAppSettingsRepository(
        implementation: AndroidAppSettingsStore
    ): AppSettingsRepository

    @Binds
    @Singleton
    abstract fun bindAppVersionProvider(
        impl: AndroidAppVersionProvider
    ): AppVersionProvider
}

@Module
@InstallIn(SingletonComponent::class)
object UseCaseModule {

    @Provides
    @Singleton
    fun provideObserveDeviceBindingUseCase(
        repository: DeviceAuthRepository
    ): ObserveDeviceBindingUseCase = ObserveDeviceBindingUseCase(repository)

    @Provides
    @Singleton
    fun provideValidateActivationCodeUseCase(
        repository: DeviceAuthRepository
    ): ValidateActivationCodeUseCase = ValidateActivationCodeUseCase(repository)

    @Provides
    @Singleton
    fun provideActivateDeviceUseCase(
        repository: DeviceAuthRepository
    ): ActivateDeviceUseCase = ActivateDeviceUseCase(repository)

    @Provides
    @Singleton
    fun provideOpenDeviceSessionUseCase(
        repository: DeviceAuthRepository
    ): OpenDeviceSessionUseCase = OpenDeviceSessionUseCase(repository)

    @Provides
    @Singleton
    fun provideRevokeLocalDeviceUseCase(
        repository: DeviceAuthRepository
    ): RevokeLocalDeviceUseCase = RevokeLocalDeviceUseCase(repository)

    @Provides
    @Singleton
    fun provideObserveDeviceSecurityUseCase(
        deviceSecurityChecker: DeviceSecurityChecker
    ): ObserveDeviceSecurityUseCase = ObserveDeviceSecurityUseCase(deviceSecurityChecker)

    @Provides
    @Singleton
    fun provideObserveAppSettingsUseCase(
        repository: AppSettingsRepository
    ): ObserveAppSettingsUseCase = ObserveAppSettingsUseCase(repository)

    @Provides
    @Singleton
    fun provideSetThemePreferenceUseCase(
        repository: AppSettingsRepository
    ): SetThemePreferenceUseCase = SetThemePreferenceUseCase(repository)

    @Provides
    @Singleton
    fun provideSetBiometricPreferenceUseCase(
        repository: AppSettingsRepository
    ): SetBiometricPreferenceUseCase = SetBiometricPreferenceUseCase(repository)
}

@Module
@InstallIn(SingletonComponent::class)
object BackendConfigModule {

    @Provides
    @Singleton
    fun provideBackendRuntimeConfig(): BackendRuntimeConfig =
        BackendRuntimeConfig(
            mode = BackendMode.from(BuildConfig.BACKEND_MODE),
            baseUrl = BuildConfig.BACKEND_BASE_URL
        )

    @Provides
    @Singleton
    fun providePostgresBackendRemoteDataSource(
        config: BackendRuntimeConfig
    ): PostgresBackendRemoteDataSource = PostgresBackendRemoteDataSource(config)

    @Provides
    @Singleton
    fun provideBackendRemoteDataSourceImpl(
        config: BackendRuntimeConfig,
        postgresBackendRemoteDataSource: PostgresBackendRemoteDataSource
    ): BackendRemoteDataSourceImpl =
        BackendRemoteDataSourceImpl(
            config = config,
            postgresBackendRemoteDataSource = postgresBackendRemoteDataSource
        )
}
