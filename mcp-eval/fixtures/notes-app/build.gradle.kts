plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.kotlinSerialization)
}

// Notes — a small Kotlin Multiplatform app (Android, iOS, Desktop JVM) built with Compose
// Multiplatform. Notes are created, listed, edited and deleted. Storage is currently in-memory.
kotlin {
    androidTarget()
    iosArm64()
    iosSimulatorArm64()
    jvm("desktop")

    sourceSets {
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.multiplatform.markdown.renderer.m3)
        }
    }
}
