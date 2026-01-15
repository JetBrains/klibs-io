package io.klibs.core.pckg.model

/**
 * BEWARE that this class is serialized and stored as JSON. Any changes must be checked.
 */
data class Configuration(
    val projectSettings: ProjectSettings,

    val jvmPlatform: JvmPlatform? = null,
    val androidJvmPlatform: AndroidJvmPlatform? = null,
    val nativePlatform: NativePlatform? = null,
    val wasmPlatform: WasmPlatform? = null,
    val jsPlatform: JsPlatform? = null,
) {
    data class ProjectSettings(
        val isHmppEnabled: Boolean,
        val isCompatibilityMetadataVariantEnabled: Boolean,
    )

    data class NativePlatform(
        val konanVersion: String,
        val konanAbiVersion: String
    )

    data class AndroidJvmPlatform(
        val sourceCompatibility: String,
        val targetCompatibility: String
    )

    data class JvmPlatform(
        val jvmTarget: String?, // TODO check the db, might never be null
        val withJavaEnabled: Boolean
    )

    data class JsPlatform(
        val isBrowserConfigured: Boolean,
        val isNodejsConfigured: Boolean
    )

    data class WasmPlatform(
        val isBrowserConfigured: Boolean,
        val isNodejsConfigured: Boolean
    )
}