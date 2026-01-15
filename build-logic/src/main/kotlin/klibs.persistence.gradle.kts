plugins {
    id("klibs.spring")
    alias(libs.plugins.kotlinSpringJpa)
}

dependencies {
    implementation(libs.spring.boot.starter.jdbc)
    implementation(libs.spring.boot.starter.data.jpa)
    implementation(libs.liquibase.core)
    implementation(libs.postgresql)

    testImplementation(libs.bundles.testcontainers)
}
