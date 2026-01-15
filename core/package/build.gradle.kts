plugins {
    id("klibs.spring-web")
    id("klibs.persistence")
}

dependencies {
    implementation(projects.integrations.ai)
    implementation(projects.integrations.maven)
}
