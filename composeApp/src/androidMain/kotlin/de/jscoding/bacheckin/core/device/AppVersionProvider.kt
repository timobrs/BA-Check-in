package de.jscoding.bacheckin.core.device

interface AppVersionProvider {
    val versionName: String
    val versionCode: Long
}
