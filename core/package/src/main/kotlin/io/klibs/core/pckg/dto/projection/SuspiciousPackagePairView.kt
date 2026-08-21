package io.klibs.core.pckg.dto.projection

import java.time.Instant

interface SuspiciousPackagePairView {
    val projectId: Int
    val artifactId: String
    val groupId: String
    val versionCount: Int
    val firstReleaseTs: Instant
    val lastReleaseTs: Instant
}

class SuspiciousPackagePair(
    override val projectId: Int,
    override val artifactId: String,
    override val groupId: String,
    override val versionCount: Int,
    override val firstReleaseTs: Instant,
    override val lastReleaseTs: Instant,
) : SuspiciousPackagePairView
