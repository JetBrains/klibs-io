package io.klibs.app.indexing

import io.klibs.app.indexing.discoverer.PackageDiscoverer
import io.klibs.app.util.ANDROIDX_OWNER_AND_GITHUB_REPOSITORY
import io.klibs.app.util.isAndroidxProject
import io.klibs.app.util.normalizeGitHubLink
import io.klibs.app.util.parseGitHubLink
import io.klibs.app.util.toIndexRequest
import io.klibs.core.pckg.repository.PackageRepository
import io.klibs.core.pckg.service.PackageService
import io.klibs.core.pckg.entity.IndexingRequestEntity
import io.klibs.core.pckg.model.Configuration
import io.klibs.core.pckg.dto.PackageDTO
import io.klibs.core.pckg.model.PackageDeveloper
import io.klibs.core.pckg.model.PackageLicense
import io.klibs.core.pckg.model.PackagePlatform
import io.klibs.core.pckg.model.PackageTarget
import io.klibs.core.pckg.repository.IndexingRequestRepository
import io.klibs.core.project.ProjectEntity
import io.klibs.core.scm.repository.ScmRepositoryEntity
import io.klibs.integration.ai.PackageDescriptionGenerator
import io.klibs.integration.maven.MavenArtifact
import io.klibs.integration.maven.MavenPom
import io.klibs.integration.maven.MavenStaticDataProvider
import io.klibs.integration.maven.delegate.KotlinToolingMetadataDelegate
import io.klibs.integration.maven.delegate.KotlinToolingMetadataDelegateImpl
import io.klibs.integration.maven.delegate.KotlinToolingMetadataDelegateStubImpl
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
import org.jetbrains.kotlin.tooling.KotlinToolingMetadata
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class PackageIndexingService(
    private val discoverers: List<PackageDiscoverer>,
    private val providers: Map<String, MavenStaticDataProvider>,
    private val gitHubIndexingService: GitHubIndexingService,
    private val projectIndexingService: ProjectIndexingService,
    private val packageDescriptionGenerator: PackageDescriptionGenerator,

    private val indexingRequestRepository: IndexingRequestRepository,
    private val packageService: PackageService,
    private val packageRepository: PackageRepository,

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
     * Processes the package queue by retrieving requests for indexing and handling them.
     *
     * @return true if a package indexing request was processed, false if the queue is empty.
     */
    @Transactional
    fun processPackageQueue(): Boolean {
        try {
            val indexRequest = indexingRequestRepository.findFirstForIndexing()
            if (indexRequest == null) {
                logger.info("The package index queue is empty")
                return false
            } else {
                processRequest(indexRequest)
                logger.debug("Processed an indexing request for {}", indexRequest)
            }
        } catch (e: Exception) {
            logger.error("Error during the queue processing: ${e.message}")
        }
        return true
    }

    private fun processRequest(indexRequest: IndexingRequestEntity) {
        try {
            val isIndividualArtifact = indexRequest.version != null
            if (isIndividualArtifact) {
                indexArtifact(indexRequest)
            } else {
                logger.error("Multi-version indexing requests are not supported")
            }
            indexingRequestRepository.deleteById(indexRequest.idNotNull)
        } catch (e: Exception) {
            logger.error("Unable to process the index request, marking the request as failed: $indexRequest", e)
            indexingRequestRepository.markAsFailed(indexRequest.idNotNull, e.message)
        }
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
        if (indexRequest.reindex) {
            packageService.updateByCoordinates(packageDto)
                ?: error("Unable to update a non-existing artifact: $mavenArtifact")
        } else {
            packageRepository.save(packageDto.toEntity())
        }
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
        val (ownerLogin, name) = pom.extractGitHubRepoInfo() ?: return null
        return gitHubIndexingService.indexRepository(ownerLogin, name)
    }

    /**
     * @return owner name to repo name
     */
    private fun MavenPom.extractGitHubRepoInfo(): Pair<String, String>? {
        val parsedGitHubLink = scm?.url?.let(::parseGitHubLink) ?: url?.let(::parseGitHubLink)
        if (parsedGitHubLink != null) return parsedGitHubLink

        val isAndroidx = scm?.url?.isAndroidxProject() == true || url?.isAndroidxProject() == true
        return if (isAndroidx) ANDROIDX_OWNER_AND_GITHUB_REPOSITORY else null
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
            developers = pom.extractDevelopers(),
            licenses = pom.extractLicenses(),
            configuration = toolingMetadata.toPackageConfiguration(),
            generatedDescription = descriptionWasGenerated,
            targets = toolingMetadata.projectTargets.map { it.toPackageTarget() }
                .distinct()
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

    private fun MavenPom.extractDevelopers(): List<PackageDeveloper> {
        val developers = this.developers ?: return emptyList()

        return developers.mapNotNull { dev ->
            val name = (dev.name ?: dev.organization)
                ?.takeIf { it.isNotBlank() }
                ?: return@mapNotNull null

            PackageDeveloper(
                name = name,
                url = dev.url ?: dev.email?.let { "mailto:$it" } ?: dev.organizationUrl
            )
        }
    }

    private fun MavenPom.extractLicenses(): List<PackageLicense> {
        val licenses = this.licenses ?: return emptyList()
        return licenses.mapNotNull { license ->
            val name = license.name?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
            PackageLicense(
                name = name,
                url = license.url
            )
        }
    }

    private fun KotlinToolingMetadataDelegate.toPackageConfiguration(): Configuration? {
        return when (this) {
            is KotlinToolingMetadataDelegateStubImpl -> null
            is KotlinToolingMetadataDelegateImpl -> Configuration(
                projectSettings = kotlinToolingMetadata.extractProjectSettings(),
                jvmPlatform = kotlinToolingMetadata.extractJvmPlatformConfiguration(),
                androidJvmPlatform = kotlinToolingMetadata.extractAndroidJvmPlatformConfiguration(),
                nativePlatform = kotlinToolingMetadata.extractNativePlatformConfiguration(),
                wasmPlatform = kotlinToolingMetadata.extractWasmPlatformConfiguration(),
                jsPlatform = kotlinToolingMetadata.extractJsPlatformConfiguration(),
            )
        }
    }

    private fun KotlinToolingMetadata.extractProjectSettings(): Configuration.ProjectSettings {
        return Configuration.ProjectSettings(
            isHmppEnabled = this.projectSettings.isHmppEnabled,
            isCompatibilityMetadataVariantEnabled = this.projectSettings.isCompatibilityMetadataVariantEnabled,
        )
    }

    private fun KotlinToolingMetadata.extractJvmPlatformConfiguration(): Configuration.JvmPlatform? {
        val jvmTarget = this.projectTargets.firstOrNull { it.platformType == "jvm" } ?: return null
        val jvmExtras = jvmTarget.extras.jvm ?: return null
        return Configuration.JvmPlatform(
            jvmTarget = jvmExtras.jvmTarget,
            withJavaEnabled = jvmExtras.withJavaEnabled
        )
    }

    private fun KotlinToolingMetadata.extractAndroidJvmPlatformConfiguration(): Configuration.AndroidJvmPlatform? {
        val androidJvmTarget = this.projectTargets.firstOrNull { it.platformType == "androidJvm" } ?: return null
        val androidJvmExtras = androidJvmTarget.extras.android ?: return null
        return Configuration.AndroidJvmPlatform(
            sourceCompatibility = androidJvmExtras.sourceCompatibility,
            targetCompatibility = androidJvmExtras.targetCompatibility
        )
    }

    private fun KotlinToolingMetadata.extractNativePlatformConfiguration(): Configuration.NativePlatform? {
        val nativeTargets = this.projectTargets.filter { it.platformType == "native" }
        val chosenExtras = nativeTargets.firstNotNullOfOrNull { it.extras.native } ?: return null

        val konanVersionsMatch = nativeTargets.all { isSameKonanVersion(it.extras.native, chosenExtras) }
        require(konanVersionsMatch) {
            "Konan configuration differs within one package: $this"
        }

        return Configuration.NativePlatform(
            konanVersion = chosenExtras.konanVersion,
            konanAbiVersion = chosenExtras.konanAbiVersion
        )
    }

    private fun isSameKonanVersion(
        first: KotlinToolingMetadata.ProjectTargetMetadata.NativeExtras?,
        second: KotlinToolingMetadata.ProjectTargetMetadata.NativeExtras?
    ): Boolean {
        return first?.konanVersion == second?.konanVersion
                && first?.konanAbiVersion == second?.konanAbiVersion
    }

    private fun KotlinToolingMetadata.extractWasmPlatformConfiguration(): Configuration.WasmPlatform? {
        val wasmTarget = this.projectTargets.firstOrNull { it.platformType == "wasm" } ?: return null
        val wasmExtras = wasmTarget.extras.js ?: return null
        return Configuration.WasmPlatform(
            isBrowserConfigured = wasmExtras.isBrowserConfigured,
            isNodejsConfigured = wasmExtras.isNodejsConfigured
        )
    }

    private fun KotlinToolingMetadata.extractJsPlatformConfiguration(): Configuration.JsPlatform? {
        val jsTarget = this.projectTargets.firstOrNull { it.platformType == "js" } ?: return null
        val jsExtras = jsTarget.extras.js ?: return null
        return Configuration.JsPlatform(
            isBrowserConfigured = jsExtras.isBrowserConfigured,
            isNodejsConfigured = jsExtras.isNodejsConfigured
        )
    }

    private fun KotlinToolingMetadata.ProjectTargetMetadata.toPackageTarget(): PackageTarget {
        return PackageTarget(
            platform= toPlatform(),
            target = when (platformType) {
                "common" -> null
                "jvm" -> if (target == "com.android.build.api.variant.impl.KotlinMultiplatformAndroidLibraryTargetImpl") {
                    // AGP 8.2-8.12
                    extractAndroidTargetCompatibility()
                } else {
                    extras.jvm?.jvmTarget
                }
                "androidJvm" -> extras.android?.targetCompatibility
                "wasm" -> null
                "native" -> extras.native?.konanTarget
                "js" -> null
                else -> error("Unknown platform type: $platformType")
            }
        )
    }

    private fun extractAndroidTargetCompatibility(): String {
        // Safe option, too hard to extract actual compatibility
        return "1.8"
    }

    private fun KotlinToolingMetadata.ProjectTargetMetadata.toPlatform(): PackagePlatform {
        return when (this.platformType) {
            "common" -> PackagePlatform.COMMON
            "jvm" -> if (target == "com.android.build.api.variant.impl.KotlinMultiplatformAndroidLibraryTargetImpl") {
                // AGP 8.2-8.12
                PackagePlatform.ANDROIDJVM
            } else {
                PackagePlatform.JVM
            }
            "androidJvm" -> PackagePlatform.ANDROIDJVM
            "wasm" -> PackagePlatform.WASM
            "native" -> PackagePlatform.NATIVE
            "js" -> PackagePlatform.JS
            else -> error("Unknown platform type: ${this.platformType}")
        }
    }

    private companion object {
        private val logger = LoggerFactory.getLogger(PackageIndexingService::class.java)
    }
}
