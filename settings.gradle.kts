@file:Suppress("UnstableApiUsage")

rootProject.name = "klibs"

pluginManagement {
    includeBuild("build-settings-logic")
    includeBuild("build-logic")

    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version ("1.0.0")
    id("klibs-build-scan")
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()

        maven(url = "https://repo.spring.io/milestone")
        maven(url = "https://repo.spring.io/snapshot")
    }
}

include(
    ":app",

    ":core:package",
    ":core:project",
    ":core:scm-owner",
    ":core:scm-repository",
    ":core:search",
    ":core:storage",

    ":integrations:ai",
    ":integrations:github",
    ":integrations:maven",
)

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")
