package io.klibs.integration.maven

enum class ScraperType {
    SEARCH_MAVEN, // search.maven.org
    GOOGLE_MAVEN, // maven.google.com
    CENTRAL_SONATYPE, // central.sonatype.com
    MANUAL_REQUEST // run on request using CentralSonatypeSeachClient
}