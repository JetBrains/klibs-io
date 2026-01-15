plugins {
    id("klibs.spring")
    id("klibs.mock")
}

dependencies {
    api(platform(libs.spring.ai.bom))
    api(libs.spring.ai.starter.model.openai)
}
