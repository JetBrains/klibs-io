plugins {
    id("klibs.spring")

    alias(libs.plugins.serialization)
}

dependencies {
    api(libs.maven.model)

    implementation(libs.spring.webflux)

    api(libs.xml.util)

    api(libs.kotlinx.serialization.json)

    api(libs.kotlin.toolingMetadata)

    implementation(libs.bucket4j)

    implementation(libs.bundles.maven.indexer)

    testImplementation(libs.mockito.kotlin)
}
