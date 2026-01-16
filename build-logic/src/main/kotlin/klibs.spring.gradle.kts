import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("klibs.kotlin-jvm")
    alias(libs.plugins.springBoot)
    alias(libs.plugins.springDependencyManagement)
    alias(libs.plugins.kotlinSpring)
}

tasks.withType<KotlinCompile> {
    @Suppress("DEPRECATION")
    kotlinOptions {
        // https://kotlinlang.org/docs/java-interop.html#jsr-305-support
        freeCompilerArgs += "-Xjsr305=strict"
    }
}

// Subprojects aren't runnable by default
tasks.bootJar {
    enabled = false
}

dependencies {
    implementation(libs.spring.boot.starter)
    implementation(libs.spring.boot.starter.logging)
    implementation(libs.spring.boot.starter.actuator)
    implementation(libs.spring.boot.docker.compose)
    implementation(libs.prometheus)

    implementation(libs.kotlinx.coroutines.jvm)
    implementation(libs.kotlin.reflect)
    implementation(libs.jackson.module.kotlin)

    testImplementation(libs.kotlinx.coroutines.test.jvm)
    testImplementation(libs.spring.boot.starter.test) {
        exclude(group = "org.mockito")
    }
}
