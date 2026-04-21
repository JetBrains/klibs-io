plugins {
    id("klibs.spring")
    id("klibs.mock")
    kotlin("kapt")
}

dependencies {
    api(platform(libs.spring.ai.bom))
    api(libs.spring.ai.starter.mcp.server.webmvc)

    implementation(projects.core.`package`)
    implementation(projects.core.search)
    implementation(projects.core.project)
    implementation(projects.core.scmOwner)
    implementation(projects.core.readme)

    implementation(libs.mapstruct)
    kapt(libs.mapstruct.processor)
}

kapt {
    arguments {
        arg("mapstruct.defaultComponentModel", "spring")
    }
}
