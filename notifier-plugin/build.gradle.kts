plugins {
    `kotlin-dsl`
}

group = "io.klibs.notifier"
version = "1.0.0"

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
    plugins {
        create("klibsIoNotifier") {
            id = "io.klibs.notifier"
            implementationClass = "io.klibs.notifier.KlibsIoNotifierPlugin"
            displayName = "Klibs.io Notifier"
            description = "Notifies klibs.io about artifacts published to Maven Central"
        }
    }
}

tasks.test {
    useJUnitPlatform()
    systemProperty("test.kotlinVersion", libs.versions.kotlin.get())
    systemProperty("test.vanniktechVersion", libs.versions.vanniktech.maven.publish.get())
}
