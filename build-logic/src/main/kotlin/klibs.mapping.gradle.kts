plugins {
    id("klibs.kotlin-jvm")
    kotlin("kapt")
}

dependencies {
    implementation(libs.mapstruct)
    kapt(libs.mapstruct.processor)
}

kapt {
    arguments {
        arg("mapstruct.defaultComponentModel", "spring")
    }
}

tasks.withType<org.jetbrains.kotlin.gradle.internal.KaptTask>().configureEach {
    if (name.contains("Test")) {
        enabled = false
    }
}