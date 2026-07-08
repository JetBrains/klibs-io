plugins {
    id("klibs.spring")
    id("klibs.mock")
}

dependencies {
    api(platform(libs.spring.ai.bom))
    api(libs.spring.ai.starter.model.openai)

    // local, fully-offline sentence-embedding model (see BgeLargeLocalEmbedder)
    implementation(platform(libs.djl.bom))
    implementation(libs.djl.api)
    implementation(libs.djl.huggingface.tokenizers)
    runtimeOnly(libs.djl.pytorch.engine)
}
