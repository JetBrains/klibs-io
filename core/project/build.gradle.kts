plugins {
    id("klibs.spring-web")
    id("klibs.persistence")
    id("klibs.mock")
}

dependencies {
    implementation(projects.core.`package`)
    implementation(projects.core.readme)
    implementation(projects.core.scmOwner)
    implementation(projects.core.scmRepository)
}
