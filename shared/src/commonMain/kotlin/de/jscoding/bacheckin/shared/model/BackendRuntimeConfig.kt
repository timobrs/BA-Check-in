package de.jscoding.bacheckin.shared.model

data class BackendRuntimeConfig(
    val mode: BackendMode,
    val baseUrl: String
) {
    val normalizedBaseUrl: String = baseUrl.trim().trimEnd('/')
}

enum class BackendMode {
    DEMO,
    POSTGRES_API;

    companion object {
        fun from(value: String): BackendMode =
            entries.firstOrNull { it.name.equals(value.trim(), ignoreCase = true) } ?: DEMO
    }
}
