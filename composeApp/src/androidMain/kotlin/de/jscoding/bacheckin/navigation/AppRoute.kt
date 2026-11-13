package de.jscoding.bacheckin.navigation

import kotlinx.serialization.Serializable

sealed interface AppRoute {
    @Serializable
    data class Launch(val code: String? = null) : AppRoute

    @Serializable
    data class ActivationGraph(val code: String? = null) : AppRoute

    @Serializable
    data class Activation1Code(val code: String? = null) : AppRoute

    @Serializable
    data object Activation2PinSelect : AppRoute

    @Serializable
    data object Activation3PinValidate : AppRoute

    @Serializable
    data object Activation4BiometricSelect : AppRoute

    @Serializable
    data class Unlock(val showAlreadyActivatedWarning: Boolean = false) : AppRoute

    @Serializable
    data object RevokedDevice : AppRoute

    @Serializable
    data object Home : AppRoute

    @Serializable
    data object Settings : AppRoute
}
