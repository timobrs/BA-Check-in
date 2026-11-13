package de.jscoding.bacheckin

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform