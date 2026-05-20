package io.klibs.app.indexing

import io.klibs.app.indexing.discoverer.PackageDiscoverer
import io.klibs.app.util.normalizeGitHubLink
import io.klibs.app.util.toIndexRequest
import io.klibs.core.pckg.dto.PackageDTO
import io.klibs.core.pckg.entity.IndexingRequestEntity
import io.klibs.core.pckg.enums.IndexingRequestStatus
import io.klibs.core.pckg.dto.MavenCoordinatesDTO
import io.klibs.core.pckg.enums.VersionType
import io.klibs.core.pckg.repository.IndexingRequestRepository
import io.klibs.core.pckg.repository.PackageRepository
import io.klibs.core.pckg.service.MavenArtifactService
import io.klibs.core.pckg.service.PackageService
import io.klibs.core.project.ProjectEntity
import io.klibs.core.scm.repository.ScmRepositoryEntity
import io.klibs.integration.ai.PackageDescriptionGenerator
import io.klibs.integration.maven.MavenArtifact
import io.klibs.integration.maven.MavenPom
import io.klibs.integration.maven.MavenStaticDataProvider
import io.klibs.integration.maven.delegate.KotlinToolingMetadataDelegate
import jakarta.transaction.Transactional
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.chunked
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.supervisorScope
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.ObjectProvider
import org.springframework.stereotype.Service

@Service
class PackageIndexingService(
    private val discoverers: List<PackageDiscoverer>,
    private val providers: Map<String, MavenStaticDataProvider>,
    private val gitHubIndexingService: GitHubIndexingService,
    private val projectIndexingService: ProjectIndexingService,
    private val pomIndexingService: PomIndexingService,
    private val kotlinToolingMetadataIndexingService: KotlinToolingMetadataIndexingService,
    private val packageDescriptionGenerator: PackageDescriptionGenerator,

    private val indexingRequestRepository: IndexingRequestRepository,
    private val packageService: PackageService,
    private val packageRepository: PackageRepository,
    private val mavenArtifactService: MavenArtifactService,
    private val selfProvider: ObjectProvider<PackageIndexingService>
) {

    @OptIn(ExperimentalCoroutinesApi::class)
    fun indexNewPackages() {
        logger.info("=== Starting scheduled packages indexing job ===")
        runBlocking {
            val errorChannel = Channel<Exception>(Channel.BUFFERED)
            try {
                val errorFlow = errorChannel.receiveAsFlow()
                launch {
                    errorFlow.collect { error ->
                        logger.error("Indexing error: ${error.message}", error.cause)
                    }
                    logger.info("=== Packages indexing: error flow completed ===")
                }

                supervisorScope {
                    discoverers.map { discoverer ->
                        launch(Dispatchers.IO) {
                            discoverer.discover(errorChannel = errorChannel)
                                .buffer()  // Allows the flow to emit faster than a collection
                                .chunked(size = 5)
                                .collect { newArtifacts ->
                                    if (newArtifacts.isNotEmpty()) {
                                        val indexRequests = newArtifacts.map { it.toIndexRequest() }
                                        val insertedRequests = indexingRequestRepository.saveAll(indexRequests).count()
                                        val removedRepeating = indexingRequestRepository.removeRepeating()
                                        logger.debug("Queued up ${insertedRequests - removedRepeating} newArtifacts")
                                    }
                                }
                        }
                    }.joinAll()
                }
            } catch (ex: Exception) {
                logger.error("Unable to process all packages for indexing", ex)
            } finally {
                errorChannel.close()
            }

        }
        logger.info("=== Finished scheduled packages indexing job ===")
    }

    /**
     * Processes the package queue by claiming a package index request and processing it.
     *
     * It finds the highest-priority pending request, which selects PENDING requests ordered by
     * release timestamp. The request is then atomically claimed, which updates
     * its status to IN_PROCESS within a separate transaction.
     *
     * If an error occurs during processing, the request is marked as FAILED and its retry counter
     * is incremented.
     *
     * @return true if a package indexing request was processed, false if the queue is empty.
     */
    fun processPackageQueue(): Boolean {
        var indexRequestId: Long? = null
        try {
            val indexRequest = indexingRequestRepository.findFirstForIndexing()
            if (indexRequest == null) {
                logger.info("The package index queue is empty")
                return false
            } else {
                indexRequestId = indexRequest.idNotNull
                selfProvider.getObject().processRequest(indexRequestId)
            }
        } catch (e: Exception) {
            logger.error("Error during claiming an indexing request: ${e.message}", e)
            try {
                indexRequestId?.let { indexingRequestRepository.markAsFailed(it, e.message) }
            } catch (ex: Exception) {
                logger.error("Error during marking index request with id=$indexRequestId: ${ex.message}", ex)
            }
        }
        return true
    }

    @Transactional
    protected fun processRequest(idToProcess: Long) {
        val indexRequest =
            indexingRequestRepository.updateStatus(idToProcess, IndexingRequestStatus.IN_PROCESS) ?: return

        val isIndividualArtifact = indexRequest.version != null
        if (isIndividualArtifact) {
            indexArtifact(indexRequest)
        } else {
            logger.error("Multi-version indexing requests are not supported")
        }
        indexingRequestRepository.deleteById(indexRequest.idNotNull)
        logger.debug("Processed an indexing request for {}", indexRequest)
    }

    private fun indexArtifact(indexRequest: IndexingRequestEntity) {
        var mavenArtifact = indexRequest.getMavenArtifact()

        logger.trace("Getting pom of {}", mavenArtifact)
        val provider: MavenStaticDataProvider = providers[mavenArtifact.scraperType.name]
            ?: throw IllegalArgumentException("Unknown repository id ${mavenArtifact.scraperType.name}")

        val (pom, releasedAt) =
            provider.getPomWithReleaseDate(mavenArtifact)
                ?: error("Unable to find the .pom for ${provider.getPomUrl(mavenArtifact)}")

        if (mavenArtifact.releasedAt == null) {
            mavenArtifact = mavenArtifact.copy(releasedAt = releasedAt)
            logger.trace("Set releasedAt for {}", mavenArtifact)
        }

        logger.trace("Indexing GitHub info of {}", mavenArtifact)
        val gitHubRepoEntity = indexGitHubInfoIfPresent(pom)

        logger.trace("Upserting a project for {}", mavenArtifact)
        val project = gitHubRepoEntity?.let {
            projectIndexingService.save(
                mavenArtifact = mavenArtifact,
                scmRepositoryEntity = it
            )
        }

        logger.trace("Getting tooling metadata for {}", mavenArtifact)
        val toolingMetadata = provider.getKotlinToolingMetadata(mavenArtifact)
            ?: error("Unable to find tooling metadata for $mavenArtifact")

        logger.trace("Persisting the package for {}", indexRequest)
        val packageDto = constructPackage(mavenArtifact, pom, toolingMetadata, project)
        val savedPackageId = if (indexRequest.reindex) {
            val updated = packageService.updateByCoordinates(packageDto)
                ?: error("Unable to update a non-existing artifact: $mavenArtifact")
            updated.id
        } else {
            val mavenArtifactDto = mavenArtifactService.resolveOrCreate(
                MavenCoordinatesDTO(pom.groupId, pom.artifactId, pom.version)
            )
            packageRepository.save(packageDto.toEntity(mavenArtifactDto)).id
        }

        logger.trace("Extracting dependencies for {}", indexRequest)
        pomIndexingService.indexDependencies(pom, requireNotNull(savedPackageId), indexRequest.reindex)
    }

    private fun IndexingRequestEntity.getMavenArtifact(): MavenArtifact {
        return MavenArtifact(
            groupId = this.groupId,
            artifactId = this.artifactId,
            version = requireNotNull(this.version) {
                "Request's version is set to null, unable to convert to MavenArtifact: $this"
            },
            scraperType = requireNotNull(this.repo) {
                "Request's repoId is set to null, unable to convert to MavenArtifact: $this"
            },
            releasedAt = this.releasedAt
        )
    }

    private fun indexGitHubInfoIfPresent(pom: MavenPom): ScmRepositoryEntity? {
        // TODO an older version might not have the GitHub link set, but a newer one might have it. add a check
        val (ownerLogin, name) = pomIndexingService.extractGitHubRepoInfo(pom) ?: return null
        return gitHubIndexingService.indexRepository(ownerLogin, name)
    }

    private fun constructPackage(
        mavenArtifact: MavenArtifact,
        pom: MavenPom,
        toolingMetadata: KotlinToolingMetadataDelegate,
        projectEntity: ProjectEntity?
    ): PackageDTO {
        val (description, descriptionWasGenerated) = resolvePackageDescription(pom)

        return PackageDTO(
            projectId = projectEntity?.idNotNull,
            repo = mavenArtifact.scraperType,
            groupId = pom.groupId,
            artifactId = pom.artifactId,
            version = pom.version,
            releaseTs = requireNotNull(mavenArtifact.releasedAt) {
                "releasedAt is null for $mavenArtifact"
            },
            description = description,
            url = pom.url?.let { normalizeGitHubLink(it) },
            scmUrl = pom.scm?.url?.let { normalizeGitHubLink(it) },
            buildTool = toolingMetadata.buildSystem,
            buildToolVersion = toolingMetadata.buildSystemVersion,
            kotlinVersion = toolingMetadata.kotlinVersion,
            developers = pomIndexingService.extractDevelopers(pom),
            licenses = pomIndexingService.extractLicenses(pom),
            configuration = kotlinToolingMetadataIndexingService.toPackageConfiguration(toolingMetadata),
            generatedDescription = descriptionWasGenerated,
            versionType = VersionType.from(pom.version),
            targets = kotlinToolingMetadataIndexingService.extractTargets(toolingMetadata)
        )
    }

    private fun resolvePackageDescription(pom: MavenPom): Pair<String?, Boolean> {
        val previousVersion = packageRepository.findFirstByGroupIdAndArtifactIdOrderByReleaseTsDesc(
            pom.groupId, pom.artifactId
        )

        // If a previous version exists and had a generated description, generate a new description
        var description = pom.description
        var descriptionWasGenerated = false

        if (previousVersion != null && previousVersion.generatedDescription) {
            try {
                description = packageDescriptionGenerator.generatePackageDescription(
                    pom.groupId,
                    pom.artifactId,
                    pom.version
                )
                descriptionWasGenerated = true
                logger.info("Generated new description for ${pom.groupId}:${pom.artifactId}:${pom.version} because previous version had a generated description")
            } catch (e: Exception) {
                logger.error("Failed to generate description for ${pom.groupId}:${pom.artifactId}:${pom.version}", e)
                // Fall back to the original description
            }
        }
        return Pair(description, descriptionWasGenerated)
    }

    private companion object {
        private val logger = LoggerFactory.getLogger(PackageIndexingService::class.java)
    }
}
