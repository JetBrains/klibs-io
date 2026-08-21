package io.klibs.core.pckg.model

enum class TargetGroup(val platform: PackagePlatform?, val targets: List<String>) {
    AndroidNative(
        PackagePlatform.NATIVE,
        listOf(
            "android_arm32",
            "android_arm64",
            "android_x64",
            "android_x86"
        )
    ),
    IOS(
        PackagePlatform.NATIVE,
        listOf(
            "ios_arm32",
            "ios_arm64",
            "ios_x64",
            "ios_simulator_arm64",
        )
    ),
    Windows(
        PackagePlatform.NATIVE,
        listOf(
            "mingw_x64",
            "mingw_x86"
        )
    ),
    MacOS(
        PackagePlatform.NATIVE,
        listOf(
            "macos_arm64",
            "macos_x64"
        )
    ),
    JavaScript(
        PackagePlatform.JS,
        listOf(
            "js_ir",
            "js_legacy",
            "js_pre_ir"
        )
    ),
    TvOS(
        PackagePlatform.NATIVE,
        listOf(
            "tvos_arm64",
            "tvos_simulator_arm64",
            "tvos_x64"
        )
    ),
    Linux(
        PackagePlatform.NATIVE,
        listOf(
            "linux_arm32_hfp",
            "linux_arm64",
            "linux_mips32",
            "linux_mipsel32",
            "linux_x64"
        )
    ),
    WatchOS(
        PackagePlatform.NATIVE,
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
        PackagePlatform.JVM,
        // Targets must be in sorted order, it is important for correct filtering further.
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
            "24",
            "25"
        )
    ),
    AndroidJvm(
        PackagePlatform.ANDROIDJVM,
        JVM.targets
    ),
    Wasm(
        PackagePlatform.WASM,
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
        fun fromPlatformAndTarget(platform: PackagePlatform, target: String?): TargetGroup? {
            return when (platform) {
                // NATIVE platform always has non-null target
                PackagePlatform.NATIVE -> entries.filter { it.platform == platform }
                    .firstOrNull() { it.targets.contains(target) } ?: Unknown

                PackagePlatform.JVM -> JVM
                PackagePlatform.WASM -> Wasm
                PackagePlatform.ANDROIDJVM -> AndroidJvm
                PackagePlatform.JS -> JavaScript
                // Common platform does not represent as a target group in our system
                PackagePlatform.COMMON -> null
            }
        }

        /**
         * Groups resolved package targets into a `TargetGroup -> Set<target>` map.
         * Targets with an undefined platform are excluded.
         */
        fun gatherTargetGroupsFromTargets(targets: List<PackageTarget>): Map<TargetGroup, Set<String>> =
            targets
                .mapNotNull { packageTarget ->
                    fromPlatformAndTarget(packageTarget.platform, packageTarget.target)
                        ?.let { key -> key to packageTarget.target }
                }
                .groupBy(
                    keySelector = { it.first },
                    valueTransform = { it.second },
                ).mapValues { it.value.filterNotNull().toSet() }

        /**
         * Groups `platform_target` strings (e.g. `NATIVE_ios_arm64`, `JVM_17`) into a `TargetGroup -> targets` map.
         * Bare tokens without a concrete target (e.g. `JS`, `COMMON`) are skipped.
         */
        fun getTargetGroupsFromTargets(platformsWithTargets: List<String>): Map<TargetGroup, Set<String>> {
            return platformsWithTargets.mapNotNull { platformWithTarget ->
                return@mapNotNull convertPlatformWithTargetToTargetGroupWithTarget(platformWithTarget)
            }
            .groupBy(
                keySelector = { it.first },
                valueTransform = { it.second },
            ).mapValues { it.value.filterNotNull().toSet() }
        }

        private fun convertPlatformWithTargetToTargetGroupWithTarget(platformWithTarget: String): Pair<TargetGroup, String?>? {
            val (platform, target) = platformWithTarget.convertPlatformWithTargetStringToPair()
            val targetGroup = fromPlatformAndTarget(platform, target) ?: return null
            return if (target.isNullOrBlank()) targetGroup to null else targetGroup to target
        }

        private fun String.convertPlatformWithTargetStringToPair(): PackageTarget {
            val platform = PackagePlatform.valueOf(this.substringBefore('_'))
            val target = this.substringAfter('_', missingDelimiterValue = "")
            return PackageTarget(platform, target)
        }
    }
}
