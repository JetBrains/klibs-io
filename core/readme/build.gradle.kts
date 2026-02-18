plugins {
    id("klibs.spring-cloud")
    id("klibs.mock")
}

dependencies {
    implementation(projects.core.storage)
    implementation(projects.integrations.github)
    implementation(libs.markdown)
}
