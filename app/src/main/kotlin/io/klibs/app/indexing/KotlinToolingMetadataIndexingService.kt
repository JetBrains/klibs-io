package io.klibs.app.indexing

import io.klibs.core.pckg.model.Configuration
import io.klibs.core.pckg.model.PackagePlatform
import io.klibs.core.pckg.model.PackageTarget
import io.klibs.integration.maven.delegate.KotlinToolingMetadataDelegate
import io.klibs.integration.maven.delegate.KotlinToolingMetadataDelegateImpl
import io.klibs.integration.maven.delegate.KotlinToolingMetadataDelegateStubImpl
import org.jetbrains.kotlin.tooling.KotlinToolingMetadata
import org.springframework.stereotype.Service

/**
 * Owns everything derived from a package's [KotlinToolingMetadataDelegate] during indexing:
 * the package-wide [Configuration] and the per-target list of [PackageTarget]s.
 */
@Service
class KotlinToolingMetadataIndexingService {

    fun toPackageConfiguration(delegate: KotlinToolingMetadataDelegate): Configuration? {
        return when (delegate) {
            is KotlinToolingMetadataDelegateStubImpl -> null
            is KotlinToolingMetadataDelegateImpl -> Configuration(
                projectSettings = delegate.kotlinToolingMetadata.extractProjectSettings(),
                jvmPlatform = delegate.kotlinToolingMetadata.extractJvmPlatformConfiguration(),
                androidJvmPlatform = delegate.kotlinToolingMetadata.extractAndroidJvmPlatformConfiguration(),
                nativePlatform = delegate.kotlinToolingMetadata.extractNativePlatformConfiguration(),
                wasmPlatform = delegate.kotlinToolingMetadata.extractWasmPlatformConfiguration(),
                jsPlatform = delegate.kotlinToolingMetadata.extractJsPlatformConfiguration(),
            )
        }
    }

    fun extractTargets(delegate: KotlinToolingMetadataDelegate): List<PackageTarget> =
        delegate.projectTargets.map { it.toPackageTarget() }.distinct()

    private fun KotlinToolingMetadata.extractProjectSettings(): Configuration.ProjectSettings {
        return Configuration.ProjectSettings(
            isHmppEnabled = this.projectSettings.isHmppEnabled,
            isCompatibilityMetadataVariantEnabled = this.projectSettings.isCompatibilityMetadataVariantEnabled,
        )
    }

    private fun KotlinToolingMetadata.extractJvmPlatformConfiguration(): Configuration.JvmPlatform? {
        val jvmTarget = this.projectTargets.firstOrNull { it.platformType == "jvm" } ?: return null
        val jvmExtras = jvmTarget.extras.jvm ?: return null
        return Configuration.JvmPlatform(
            jvmTarget = jvmExtras.jvmTarget,
            withJavaEnabled = jvmExtras.withJavaEnabled
        )
    }

    private fun KotlinToolingMetadata.extractAndroidJvmPlatformConfiguration(): Configuration.AndroidJvmPlatform? {
        val androidJvmTarget = this.projectTargets.firstOrNull { it.platformType == "androidJvm" } ?: return null
        val androidJvmExtras = androidJvmTarget.extras.android ?: return null
        return Configuration.AndroidJvmPlatform(
            sourceCompatibility = androidJvmExtras.sourceCompatibility,
            targetCompatibility = androidJvmExtras.targetCompatibility
        )
    }

    private fun KotlinToolingMetadata.extractNativePlatformConfiguration(): Configuration.NativePlatform? {
        val nativeTargets = this.projectTargets.filter { it.platformType == "native" }
        val chosenExtras = nativeTargets.firstNotNullOfOrNull { it.extras.native } ?: return null

        val konanVersionsMatch = nativeTargets.all { isSameKonanVersion(it.extras.native, chosenExtras) }
        require(konanVersionsMatch) {
            "Konan configuration differs within one package: $this"
        }

        return Configuration.NativePlatform(
            konanVersion = chosenExtras.konanVersion,
            konanAbiVersion = chosenExtras.konanAbiVersion
        )
    }

    private fun isSameKonanVersion(
        first: KotlinToolingMetadata.ProjectTargetMetadata.NativeExtras?,
        second: KotlinToolingMetadata.ProjectTargetMetadata.NativeExtras?
    ): Boolean {
        return first?.konanVersion == second?.konanVersion
                && first?.konanAbiVersion == second?.konanAbiVersion
    }

    private fun KotlinToolingMetadata.extractWasmPlatformConfiguration(): Configuration.WasmPlatform? {
        val wasmTarget = this.projectTargets.firstOrNull { it.platformType == "wasm" } ?: return null
        val wasmExtras = wasmTarget.extras.js ?: return null
        return Configuration.WasmPlatform(
            isBrowserConfigured = wasmExtras.isBrowserConfigured,
            isNodejsConfigured = wasmExtras.isNodejsConfigured
        )
    }

    private fun KotlinToolingMetadata.extractJsPlatformConfiguration(): Configuration.JsPlatform? {
        val jsTarget = this.projectTargets.firstOrNull { it.platformType == "js" } ?: return null
        val jsExtras = jsTarget.extras.js ?: return null
        return Configuration.JsPlatform(
            isBrowserConfigured = jsExtras.isBrowserConfigured,
            isNodejsConfigured = jsExtras.isNodejsConfigured
        )
    }

    private fun KotlinToolingMetadata.ProjectTargetMetadata.toPackageTarget(): PackageTarget {
        return PackageTarget(
            platform = toPlatform(),
            target = when (platformType) {
                "common" -> null
                "jvm" -> if (target == "com.android.build.api.variant.impl.KotlinMultiplatformAndroidLibraryTargetImpl") {
                    // AGP 8.2-8.12
                    extractAndroidTargetCompatibility()
                } else {
                    extras.jvm?.jvmTarget
                }

                "androidJvm" -> extras.android?.targetCompatibility
                "wasm" -> null
                "native" -> extras.native?.konanTarget
                "js" -> null
                else -> error("Unknown platform type: $platformType")
            }
        )
    }

    private fun extractAndroidTargetCompatibility(): String {
        // Safe option, too hard to extract actual compatibility
        return "1.8"
    }

    private fun KotlinToolingMetadata.ProjectTargetMetadata.toPlatform(): PackagePlatform {
        return when (this.platformType) {
            "common" -> PackagePlatform.COMMON
            "jvm" -> if (target == "com.android.build.api.variant.impl.KotlinMultiplatformAndroidLibraryTargetImpl") {
                // AGP 8.2-8.12
                PackagePlatform.ANDROIDJVM
            } else {
                PackagePlatform.JVM
            }

            "androidJvm" -> PackagePlatform.ANDROIDJVM
            "wasm" -> PackagePlatform.WASM
            "native" -> PackagePlatform.NATIVE
            "js" -> PackagePlatform.JS
            else -> error("Unknown platform type: ${this.platformType}")
        }
    }
}
