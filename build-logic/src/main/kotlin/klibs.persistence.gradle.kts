import org.jetbrains.kotlin.gradle.dsl.JvmDefaultMode

plugins {
    id("klibs.spring")
    alias(libs.plugins.kotlinSpringJpa)
}

kotlin {
    compilerOptions {
        // Required so Spring Data treats Kotlin interface default methods as real default
        // methods (otherwise Spring Data tries to parse them as derived queries and fails).
        // See https://kotlinlang.org/docs/java-to-kotlin-interop.html#default-methods-in-interfaces
        jvmDefault.set(JvmDefaultMode.NO_COMPATIBILITY)
    }
}

dependencies {
    implementation(libs.spring.boot.starter.jdbc)
    implementation(libs.spring.boot.starter.data.jpa)
    implementation(libs.spring.boot.starter.liquibase)
    implementation(libs.postgresql)

    testImplementation(libs.bundles.testcontainers)
}
