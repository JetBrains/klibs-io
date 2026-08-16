package io.klibs.integration.maven

class MavenRateLimitedException(url: String) : RuntimeException("Rate limited by Maven Central on $url")
