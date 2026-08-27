package io.klibs.integration.maven

enum class ScraperType {
    SEARCH_MAVEN, // search.maven.org
    GOOGLE_MAVEN, // maven.google.com
    GOOGLE_MAVEN_CENTRAL_MIRROR, // Google-hosted mirror of Maven Central
    CENTRAL_SONATYPE // central.sonatype.com; also used for on-demand user requests
}