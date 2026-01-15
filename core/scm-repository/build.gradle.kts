plugins {
    id("klibs.spring-web")
    id("klibs.persistence")
    id("klibs.spring-cloud")
    id("klibs.mock")
}

dependencies {
    implementation(projects.core.scmOwner)
    implementation(projects.core.storage)
    implementation(libs.markdown)
}
