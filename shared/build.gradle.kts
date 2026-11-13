import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.kotlinSerialization)
}

val generatedSecretsDir = layout.buildDirectory.dir("generated/secrets/commonMain/kotlin")
val generateSecretsClass = tasks.register("generateSecretsClass") {
    description = "Generates a Kotlin object containing the secrets from secrets.json."
    val secretsFile = rootProject.layout.projectDirectory.file("secrets.json").asFile
    val outputFile = generatedSecretsDir
        .map { it.file("de/jscoding/bacheckin/shared/Secrets.kt") }

    if (secretsFile.exists()) { inputs.file(secretsFile) }
    outputs.file(outputFile)

    doLast {
        val json = if (secretsFile.exists()) {
            secretsFile.readText()
        } else {
            System.getenv("SECRETS_JSON") ?: error("secrets.json not found and SECRETS_JSON env var not set")
        }

        outputFile.get().asFile.apply {
            parentFile.mkdirs()
            writeText("""
            package de.jscoding.bacheckin.shared

            internal object Secrets {
                val accountsJson: String = ${"\"\"\""}$json${"\"\"\""}
            }
            """.trimIndent())
        }
    }
}

tasks.matching { it.name.startsWith("compile") && it.name.contains("Kotlin") }.configureEach {
    dependsOn(generateSecretsClass)
}

kotlin {
    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }

    listOf(
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "Shared"
            isStatic = false
        }
    }

    sourceSets {
        named("commonMain") {
            kotlin.srcDir(generatedSecretsDir)
        }

        commonMain.dependencies {
            implementation(libs.ktor.client.core)
            implementation(libs.ktor.client.content.negotiation)
            implementation(libs.ktor.serialization.kotlinx.json)
            implementation(libs.ksoup)
            implementation(libs.kermit)
            implementation(libs.kotlinx.datetime)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.serialization.json)
        }

        androidMain.dependencies {
            implementation(libs.ktor.client.okhttp)
            implementation(libs.ktor.client.logging)
        }

        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
        }
    }
}

android {
    namespace = "de.jscoding.bacheckin.shared"
    compileSdk = libs.versions.android.compileSdk.get().toInt()
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    defaultConfig {
        minSdk = libs.versions.android.minSdk.get().toInt()
    }
}
