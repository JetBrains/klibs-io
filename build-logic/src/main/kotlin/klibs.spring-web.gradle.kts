plugins {
    id("klibs.spring")
}

dependencies {
    implementation(libs.spring.boot.starter.web)
    implementation(libs.spring.boot.starter.security)
    implementation(libs.spring.boot.starter.validation)

    implementation(libs.kotlinx.coroutines.reactor)

    implementation(libs.springdoc.openapi.starter)
}
