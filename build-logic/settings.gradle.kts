pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
}

@Suppress("UnstableApiUsage")
dependencyResolutionManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
    }
}

plugins {
    // it is workaround for being able to use version catalog inside convention plugins
    // see https://github.com/gradle/gradle/issues/15383#issuecomment-779893192 for more details
    id("dev.panuszewski.typesafe-conventions") version "0.5.1"
}
