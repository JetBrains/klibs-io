plugins {
    id("klibs.spring")
    alias(libs.plugins.kotlinSpringJpa)
}

dependencies {
    implementation(libs.spring.boot.starter.jdbc)
    implementation(libs.spring.boot.starter.data.jpa)
    implementation(libs.spring.boot.starter.liquibase)
    implementation(libs.postgresql)

    testImplementation(libs.bundles.testcontainers)
}
