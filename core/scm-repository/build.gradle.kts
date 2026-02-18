plugins {
    id("klibs.spring-web")
    id("klibs.persistence")
    id("klibs.mock")
}

dependencies {
    implementation(projects.core.scmOwner)
}
