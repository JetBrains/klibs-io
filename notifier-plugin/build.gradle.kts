import org.gradle.plugin.compatibility.compatibility

plugins {
    `kotlin-dsl`
    id("com.gradle.plugin-publish") version "2.2.1"
}

group = "org.jetbrains.kotlin"
version = "0.0.1"

kotlin {
    jvmToolchain(17)
}

dependencies {
    testImplementation(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)
    testRuntimeOnly(libs.junit.platform.launcher)

    testImplementation(libs.gradle.maven.publish.plugin)
    testImplementation(libs.kotlin.gradle.plugin)
}

gradlePlugin {
    website = "https://klibs.io/"
    vcsUrl = "https://github.com/JetBrains/klibs-io"
    plugins {
        create("klibs-io-notifier") {
            id = "org.jetbrains.kotlin.klibs-io-notifier"
            implementationClass = "io.klibs.notifier.KlibsIoNotifierPlugin"
            displayName = "Klibs.io Notifier"
            description = "Notifies klibs.io about Kotlin Multiplatform libraries published to Maven Central"
            tags = listOf("klibs.io", "Kotlin", "KMP", "KotlinMultiplatform", "publishing", "mavenCentral")

            compatibility {
                features {
                    configurationCache = true
                }
            }
        }
    }
}

tasks.test {
    useJUnitPlatform()
    systemProperty("test.kotlinVersion", libs.versions.kotlin.get())
    systemProperty("test.vanniktechVersion", libs.versions.vanniktech.maven.publish.get())
}
