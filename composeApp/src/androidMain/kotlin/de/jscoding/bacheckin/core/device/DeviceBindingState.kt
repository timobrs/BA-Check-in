package de.jscoding.bacheckin.core.device

import de.jscoding.bacheckin.shared.model.UserRole


enum class DeviceBindingStatus {
    UNACTIVATED,
    ACTIVATED,
    REVOKED
}

data class DeviceBindingState(
    val status: DeviceBindingStatus = DeviceBindingStatus.UNACTIVATED,
    val userId: String? = null,
    val userRole: UserRole = UserRole.NONE,
    val displayName: String? = null,
    val deviceCredentialId: String? = null,
    val accessTokenExpiresAtEpochMillis: Long? = null
) {
    val hasActiveLocalBinding: Boolean
        get() = status == DeviceBindingStatus.ACTIVATED && !deviceCredentialId.isNullOrBlank()
}
