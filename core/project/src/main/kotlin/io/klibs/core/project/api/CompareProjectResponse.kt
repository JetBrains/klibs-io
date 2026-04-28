package io.klibs.core.project.api

data class CompareProjectResponse(
    val ownerLogin: String,
    val projectName: String,
    val description: String?,
    val scmStars: Int,
    val licenseName: String?,
    val latestReleaseVersion: String?,
    val latestReleasePublishedAtMillis: Long?,
    val lastActivityAtMillis: Long,
    val createdAtMillis: Long,
    val openIssues: Int?,
    val platforms: List<String>,
    val tags: List<String>,
    val markers: List<String>,
    val kotlinVersion: String?
)
