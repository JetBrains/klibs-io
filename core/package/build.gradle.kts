plugins {
    id("klibs.spring-web")
    id("klibs.persistence")
    id("klibs.mock")
}

dependencies {
    implementation(projects.integrations.ai)
    implementation(projects.integrations.maven)
    implementation(libs.kotlin.semver)
}
