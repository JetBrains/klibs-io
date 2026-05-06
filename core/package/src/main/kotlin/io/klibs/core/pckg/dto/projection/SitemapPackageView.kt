package io.klibs.core.pckg.dto.projection

import java.time.Instant

interface SitemapPackageView {
    val groupId: String
    val artifactId: String
    val releaseTs: Instant
}
