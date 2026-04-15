package io.klibs.core.project.repository

import java.time.Instant

data class SitemapProjectEntry(
    val ownerLogin: String,
    val projectName: String,
    val updatedAt: Instant,
)
