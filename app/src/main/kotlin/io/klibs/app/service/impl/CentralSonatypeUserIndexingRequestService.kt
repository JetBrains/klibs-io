package io.klibs.app.service.impl

import io.klibs.app.configuration.properties.IndexingConfigurationProperties
import io.klibs.app.exceptions.UserRequestProcessingException
import io.klibs.app.service.UserIndexingRequestService
import io.klibs.app.util.toIndexRequest
import io.klibs.core.pckg.entity.UserRequestIssueEntity
import io.klibs.core.pckg.repository.IndexingRequestRepository
import io.klibs.core.pckg.repository.PackageRepository
import io.klibs.core.pckg.repository.UserRequestIssueRepository
import io.klibs.core.project.blacklist.BlacklistRepository
import io.klibs.integration.maven.MavenArtifact
import io.klibs.integration.maven.service.impl.SonatypeCentralStaticDataProvider
import java.util.UUID
import kotlin.jvm.optionals.getOrNull
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class CentralSonatypeUserIndexingRequestService(
    // User requests should go through real Maven Central
    private val centralSearchClient: SonatypeCentralStaticDataProvider,
    private val indexingRequestRepository: IndexingRequestRepository,
    private val packageRepository: PackageRepository,
    private val userRequestIssueRepository: UserRequestIssueRepository,
    private val blacklistRepository: BlacklistRepository,
) : UserIndexingRequestService {

    @Transactional
    override fun fulfillRequest(userRequestId: UUID) {
        val userRequestIssue = userRequestIssueRepository.findById(userRequestId).getOrNull()
            ?: throw UserRequestProcessingException("User request not found")

        fulfillRequest(
            userRequestIssue.groupId,
            userRequestIssue.artifactId,
            userRequestIssue.version,
            userRequestIssue
        )
    }

    private fun fulfillRequest(
        groupId: String,
        artifactId: String,
        version: String?,
        issue: UserRequestIssueEntity? = null,
    ) {
        val artifacts = discoverArtifacts(groupId, artifactId, version)
        saveUserRequests(artifacts, issue)
    }

    private fun discoverArtifacts(
        groupId: String,
        artifactId: String,
        version: String?,
    ): List<MavenArtifact> {
        if (version != null) {
            return listOf(resolveSpecificVersion(groupId, artifactId, version))
        }

        val foundPackages = searchForPackages(groupId, artifactId)

        val artifactsToSave = foundPackages
            .filterNot { isBanned(it) }
            .filterNot { isAlreadyIndexedOrQueued(it) }

        if (artifactsToSave.isEmpty()) throw UserRequestProcessingException("All artifacts from this request are already indexed, queued or banned")

        return artifactsToSave
    }

    private fun searchForPackages(
        groupId: String,
        artifactId: String
    ): List<MavenArtifact> {
        val mavenMetadata = centralSearchClient.getMavenMetadata(groupId, artifactId)
        return mavenMetadata
            ?.versioning?.versions
            ?.map { MavenArtifact(groupId, artifactId, it, centralSearchClient.scraperType) }
            ?.filter { artifact ->
                centralSearchClient.getKotlinToolingMetadata(artifact) != null
            }
            ?: throw UserRequestProcessingException(
                "No Kotlin Multiplatform artifacts found for $groupId${
                    artifactId.let { ":$it" }
                }"
            )
    }

    private fun resolveSpecificVersion(groupId: String, artifactId: String, version: String): MavenArtifact {
        val artifact = MavenArtifact(groupId, artifactId, version, centralSearchClient.scraperType)
        if (isBanned(artifact)) throw UserRequestProcessingException("Artifact $groupId:$artifactId:$version is banned")
        if (isAlreadyIndexedOrQueued(artifact)) throw UserRequestProcessingException("Artifact $groupId:$artifactId:$version is already indexed or queued")

        centralSearchClient.getKotlinToolingMetadata(artifact)
            ?: throw UserRequestProcessingException(
                "Artifact $groupId:$artifactId:$version is not a valid Kotlin Multiplatform library " +
                        "(kotlin-tooling-metadata.json not found)"
            )

        return artifact
    }

    private fun isBanned(artifact: MavenArtifact): Boolean =
        blacklistRepository.checkPackageBanned(artifact.groupId, artifact.artifactId).also { banned ->
            if (banned) logger.debug("Banned: ${artifact.groupId}:${artifact.artifactId}, skipping")
        }

    private fun isAlreadyIndexedOrQueued(artifact: MavenArtifact): Boolean =
        with(artifact) {
            when {
                packageRepository.findByGroupIdAndArtifactIdAndVersion(groupId, artifactId, version) != null -> {
                    logger.debug("Already indexed: $groupId:$artifactId:$version, skipping")
                    true
                }

                indexingRequestRepository.findByGroupIdAndArtifactIdAndVersion(
                    groupId,
                    artifactId,
                    version
                ) != null -> {
                    logger.debug("Already queued: $groupId:$artifactId:$version, skipping")
                    true
                }

                else -> false
            }
        }

    private fun saveUserRequests(mavenArtifacts: List<MavenArtifact>, issue: UserRequestIssueEntity? = null) {
        val requests = mavenArtifacts.map { it.toIndexRequest(userRequestIssue = issue) }

        try {
            indexingRequestRepository.saveAll(requests)
            logger.info("Saved ${requests.size} user requests")
        } catch (e: Exception) {
            logger.error("Failed to save user requests: ${e.message}")
            throw UserRequestProcessingException(
                "Failed to save your indexing request due to an internal error. Please try again later."
            )
        }
    }

    companion object {
        private val logger = LoggerFactory.getLogger(CentralSonatypeUserIndexingRequestService::class.java)
    }
}