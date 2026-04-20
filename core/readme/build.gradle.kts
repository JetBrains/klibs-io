plugins {
    id("klibs.spring-cloud")
    id("klibs.mock")
    id("klibs.persistence")
}

dependencies {
    implementation(projects.core.storage)
    implementation(projects.integrations.github)
    implementation(libs.okhttp)
    implementation(libs.markdown)
}
