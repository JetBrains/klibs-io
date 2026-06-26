plugins {
    id("klibs.spring")
    id("klibs.mock")
    id("klibs.mapping")
}

dependencies {
    api(platform(libs.spring.ai.bom))
    api(libs.spring.ai.starter.mcp.server.webmvc)

    implementation(projects.core.`package`)
    implementation(projects.core.search)
    implementation(projects.core.project)
    implementation(projects.core.scmOwner)
    implementation(projects.core.readme)
}