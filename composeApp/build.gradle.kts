import org.jetbrains.kotlin.gradle.dsl.JvmTarget

fun buildConfigString(value: String): String =
    "\"${value.replace("\\", "\\\\").replace("\"", "\\\"")}\""

val localBackendBaseUrl = providers.gradleProperty("backendLocalBaseUrl")
    .orElse(providers.environmentVariable("BACKEND_LOCAL_BASE_URL"))
    .orElse("http://10.0.2.2:8080")
val prodBackendBaseUrl = providers.gradleProperty("backendProdBaseUrl")
    .orElse(providers.environmentVariable("BACKEND_PROD_BASE_URL"))
    .orElse("https://bacheckin.brettspielrunde.com")
val pilotKeystoreFile = providers.gradleProperty("pilotKeystoreFile")
    .orElse(providers.environmentVariable("PILOT_KEYSTORE_FILE"))
val pilotKeystorePassword = providers.gradleProperty("pilotKeystorePassword")
    .orElse(providers.environmentVariable("PILOT_KEYSTORE_PASSWORD"))
val pilotKeyAlias = providers.gradleProperty("pilotKeyAlias")
    .orElse(providers.environmentVariable("PILOT_KEY_ALIAS"))
val pilotKeyPassword = providers.gradleProperty("pilotKeyPassword")
    .orElse(providers.environmentVariable("PILOT_KEY_PASSWORD"))

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
    alias(libs.plugins.kotlinSerialization)
}

kotlin {
    androidTarget {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }

    sourceSets {
        androidMain.dependencies {
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.androidx.activity.compose)
            implementation(libs.androidx.fragment.ktx)
            implementation(libs.androidx.lifecycle.runtimeCompose)
            implementation(libs.androidx.lifecycle.viewmodelCompose)

            implementation(libs.androidx.navigation.compose)
            implementation(libs.androidx.hilt.navigation.compose)
            implementation(libs.androidx.compose.animation)
            implementation(libs.androidx.compose.material.icons.core)
            implementation(libs.androidx.compose.material.icons.extended)

            implementation(libs.kotlinx.serialization.json)
            implementation(libs.androidx.material3.android)
            implementation(libs.androidx.biometric)
            implementation(libs.androidx.datastore.preferences)

            implementation(libs.androidx.camera.core)
            implementation(libs.androidx.camera.camera2)
            implementation(libs.androidx.camera.lifecycle)
            implementation(libs.androidx.camera.view)
            implementation(libs.barcode.scanning)

            implementation(libs.core)
            implementation(libs.javase)

            implementation(libs.accompanist.permissions)
        }

        commonMain.dependencies {
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.ui)
            implementation(libs.compose.components.resources)
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)
            implementation(projects.shared)
        }

    }
}

android {
    namespace = "de.jscoding.bacheckin"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "de.jscoding.bacheckin"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0"

    }
    buildFeatures {
        buildConfig = true
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
        }
    }
    val configuredPilotKeystore = pilotKeystoreFile.orNull?.let { file(it) }
    if (configuredPilotKeystore != null && configuredPilotKeystore.exists()) {
        signingConfigs {
            create("pilot") {
                storeFile = configuredPilotKeystore
                storePassword = pilotKeystorePassword.orElse("").get()
                keyAlias = pilotKeyAlias.orElse("").get()
                keyPassword = pilotKeyPassword.orElse("").get()
            }
        }
        buildTypes.getByName("release") {
            signingConfig = signingConfigs.getByName("pilot")
        }
    }
    flavorDimensions += "backend"
    productFlavors {
        create("fake") {
            dimension = "backend"
            applicationIdSuffix = ".fake"
            versionNameSuffix = "-fake"
            buildConfigField("String", "BACKEND_MODE", "\"DEMO\"")
            buildConfigField("String", "BACKEND_BASE_URL", buildConfigString("fake://in-process"))
            manifestPlaceholders["usesCleartextTraffic"] = "false"
        }
        create("local") {
            dimension = "backend"
            applicationIdSuffix = ".local"
            versionNameSuffix = "-local"
            buildConfigField("String", "BACKEND_MODE", "\"POSTGRES_API\"")
            buildConfigField("String", "BACKEND_BASE_URL", buildConfigString(localBackendBaseUrl.get()))
            manifestPlaceholders["usesCleartextTraffic"] = "true"
        }
        create("prod") {
            dimension = "backend"
            buildConfigField("String", "BACKEND_MODE", "\"POSTGRES_API\"")
            buildConfigField("String", "BACKEND_BASE_URL", buildConfigString(prodBackendBaseUrl.get()))
            manifestPlaceholders["usesCleartextTraffic"] = "false"
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

androidComponents {
    beforeVariants { variantBuilder ->
        val backendFlavor = variantBuilder.productFlavors
            .firstOrNull { (dimension, _) -> dimension == "backend" }
            ?.second
        val isSupportedVariant = when (backendFlavor) {
            "fake", "local" -> variantBuilder.buildType == "debug"
            "prod" -> variantBuilder.buildType == "release"
            else -> false
        }
        variantBuilder.enable = isSupportedVariant
    }
}

dependencies {
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    ksp("org.jetbrains.kotlin:kotlin-metadata-jvm:${libs.versions.kotlin.get()}")
    annotationProcessor("org.jetbrains.kotlin:kotlin-metadata-jvm:${libs.versions.kotlin.get()}")

    debugImplementation(libs.compose.uiTooling)
}
