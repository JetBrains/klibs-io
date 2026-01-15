package io.klibs.core.pckg.api

import io.swagger.v3.oas.annotations.media.Schema

@Schema(
    name = "PackageTarget",
    description = "Information about a package target"
)
data class PackageTargetResponse(
    @Schema(
        description = "Platforms supported by the package. Predefined values.",
        allowableValues = ["common", "jvm", "androidJvm", "native", "wasm", "js"]
    )
    val platform: String,

    @Schema(
        description = "Specific targets within the platform. The values are different based on platform. New values may be added, comes straight from tooling-metadata-info.json",
        allowableValues = [
            "macos_x64", "ios_x64", "12", "watchos_arm64", "null", "mingw_x64", "android_x64",
            "ios_arm64", "1.6", "watchos_simulator_arm64", "13", "15", "mingw_x86", "tvos_arm64",
            "ios_arm32", "19", "android_arm64", "20", "14", "watchos_device_arm64", "wasm32",
            "linux_arm32_hfp", "18", "linux_mipsel32", "android_arm32", "1.8", "tvos_simulator_arm64",
            "ios_simulator_arm64", "tvos_x64", "android_x86", "watchos_arm32", "21", "macos_arm64", "17",
            "linux_arm64", "22", "watchos_x86", "11", "linux_x64", "1.7", "watchos_x64", "linux_mips32", "16",
        ]
    )
    val target: String?
)
