package io.klibs.core.pckg.model

enum class TargetGroup(val platformName: String?, val targets: List<String>) {
    AndroidNative(
        "NATIVE",
        listOf(
            "android_arm32",
            "android_arm64",
            "android_x64",
            "android_x86"
        )
    ),
    IOS(
        "NATIVE",
        listOf(
            "ios_arm32",
            "ios_arm64",
            "ios_x64",
            "ios_simulator_arm64",
        )
    ),
    Windows(
        "NATIVE",
        listOf(
            "mingw_x64",
            "mingw_x86"
        )
    ),
    MacOS(
        "NATIVE",
        listOf(
            "macos_arm64",
            "macos_x64"
        )
    ),
    JavaScript(
        "JS",
        listOf(
            "js_ir",
            "js_legacy",
            "js_pre_ir"
        )
    ),
    TvOS(
        "NATIVE",
        listOf(
            "tvos_arm64",
            "tvos_simulator_arm64",
            "tvos_x64"
        )
    ),
    Linux(
        "NATIVE",
        listOf(
            "linux_arm32_hfp",
            "linux_arm64",
            "linux_mips32",
            "linux_mipsel32",
            "linux_x64"
        )
    ),
    WatchOS(
        "NATIVE",
        listOf(
            "watchos_arm32",
            "watchos_arm64",
            "watchos_device_arm64",
            "watchos_simulator_arm64",
            "watchos_x64",
            "watchos_x86"
        )
    ),
    JVM(
        "JVM",
        listOf(
            "1.6",
            "1.7",
            "1.8",
            "9",
            "10",
            "11",
            "12",
            "13",
            "14",
            "15",
            "16",
            "17",
            "18",
            "19",
            "20",
            "21",
            "22",
            "23",
            "24"
        )
    ),
    AndroidJvm(
        "ANDROIDJVM",
        listOf(
            "1.6",
            "1.7",
            "1.8",
            "9",
            "10",
            "11",
            "12",
            "13",
            "14",
            "15",
            "16",
            "17",
            "18",
            "19",
            "20",
            "21",
            "22",
            "23",
            "24"
        )
    ),
    Wasm(
        "NATIVE",
        listOf(
            "wasm32"
        )
    ),
    Unknown(
        null,
        emptyList()
    );

    companion object {
        /**
         * Finds the TargetGroup that contains the specified target.
         *
         * @param platform The platform to find the corresponding TargetGroup for
         * @param target The target to find the corresponding TargetGroup for
         * @return The TargetGroup that contains the target, or null if no TargetGroup contains the target
         */
        fun fromPlatformAndTarget(platform: String, target: String): TargetGroup {
            val platformMatches = entries.filter { it.platformName == platform }
            return if (target.isBlank()) {
                platformMatches.firstOrNull()
            } else {
                platformMatches.firstOrNull { it.targets.contains(target) }
            } ?: Unknown
        }
    }
}
