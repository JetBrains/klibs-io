plugins {
    id("klibs.spring")
    id("klibs.persistence")

    alias(libs.plugins.serialization)
}

dependencies {
    api(libs.maven.model)

    implementation(libs.spring.webflux)

    api(libs.xml.util)
    api(libs.jackson.dataformat.xml)

    api(libs.kotlinx.serialization.json)

    api(libs.kotlin.toolingMetadata)

    implementation(libs.bucket4j)

    implementation(libs.bundles.maven.indexer)

    testImplementation(libs.mockito.kotlin)
}
