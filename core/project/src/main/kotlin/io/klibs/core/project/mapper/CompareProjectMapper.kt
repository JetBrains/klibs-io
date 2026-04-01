package io.klibs.core.project.mapper

import io.klibs.core.project.ProjectDetails
import io.klibs.core.project.api.CompareProjectResponse
import org.springframework.stereotype.Component

@Component
class CompareProjectMapper {

    fun mapToCompareResponse(details: ProjectDetails, kotlinVersion: String?): CompareProjectResponse {
        return CompareProjectResponse(
            ownerLogin = details.ownerLogin,
            projectName = details.name,
            description = details.description,
            scmStars = details.stars,
            licenseName = details.licenseName,
            latestReleaseVersion = details.latestReleaseVersion,
            latestReleasePublishedAtMillis = details.latestReleasePublishedAt?.toEpochMilli(),
            lastActivityAtMillis = details.lastActivityAt.toEpochMilli(),
            createdAtMillis = details.createdAt.toEpochMilli(),
            openIssues = details.openIssues,
            platforms = details.platforms.map { it.serializableName },
            tags = details.tags,
            markers = details.markers.map { it.name },
            kotlinVersion = kotlinVersion,
        )
    }
}
