package io.klibs.core.project.blacklist

import SmokeTestBase
import com.fasterxml.jackson.databind.ObjectMapper
import io.klibs.core.owner.ScmOwnerEntity
import io.klibs.core.owner.ScmOwnerRepository
import io.klibs.core.owner.ScmOwnerType
import io.klibs.core.pckg.repository.PackageRepository
import io.klibs.core.pckg.service.PackageService
import io.klibs.core.pckg.model.Configuration
import io.klibs.core.pckg.dto.PackageDTO
import io.klibs.core.pckg.model.PackageDeveloper
import io.klibs.core.pckg.model.PackageLicense
import io.klibs.core.pckg.model.PackagePlatform
import io.klibs.core.pckg.model.PackageTarget
import io.klibs.core.project.ProjectEntity
import io.klibs.core.project.repository.ProjectRepository
import io.klibs.core.scm.repository.ScmRepositoryEntity
import io.klibs.core.scm.repository.ScmRepositoryRepository
import io.klibs.core.search.dto.api.SearchProjectResultDTO
import io.klibs.core.search.SearchService
import io.klibs.integration.maven.ScraperType
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.get
import java.time.Instant
import kotlin.test.AfterTest
import kotlin.test.assertTrue

@ActiveProfiles("test")
class BlacklistSmokeTest : SmokeTestBase() {

    @Autowired
    private lateinit var packageRepository: PackageRepository

    @Autowired
    private lateinit var packageService: PackageService

    @Autowired
    private lateinit var projectRepository: ProjectRepository

    @Autowired
    private lateinit var blacklistService: BlacklistService

    @Autowired
    private lateinit var searchService: SearchService

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Autowired
    private lateinit var scmOwnerRepository: ScmOwnerRepository

    @Autowired
    private lateinit var scmRepositoryRepository: ScmRepositoryRepository

    @Autowired
    private lateinit var jdbcClient: JdbcClient

    var savedPackageId: Long? = null
    var savedProjectId: Int? = null
    var savedRepoId: Int? = null
    var savedOwnerId: Int? = null

    @AfterTest
    fun cleanup() {
        // Clean up all created entities in reverse order to respect foreign key constraints
        savedPackageId?.let {
            jdbcClient.sql("DELETE FROM package WHERE id = ?").param(it).update()
        }
        savedProjectId?.let {
            jdbcClient.sql("DELETE FROM project WHERE id = ?").param(it).update()
        }
        savedRepoId?.let {
            jdbcClient.sql("DELETE FROM scm_repo WHERE id = ?").param(it).update()
        }
        savedOwnerId?.let {
            jdbcClient.sql("DELETE FROM scm_owner WHERE id = ?").param(it).update()
        }

        // Refresh search index after cleanup
        searchService.refreshSearchViews()
    }

    @Test
    fun `should ban package and make project not searchable`() {
        // Create a test owner
        val ownerEntity = ScmOwnerEntity(
            id = null,
            nativeId = 12345L,
            type = ScmOwnerType.AUTHOR,
            login = "test-user",
            name = "Test User",
            description = "Test user for smoke test",
            location = null,
            followers = 0,
            company = null,
            homepage = null,
            twitterHandle = null,
            email = null,
            updatedAtTs = Instant.now()
        )
        val savedOwner = scmOwnerRepository.upsert(ownerEntity)
        savedOwnerId = savedOwner.idNotNull

        // Create a test repository with a unique name
        val uniqueRepoName = "unique-test-repo-${System.currentTimeMillis()}"
        val repoEntity = ScmRepositoryEntity(
            id = null,
            nativeId = 12345L,
            name = uniqueRepoName,
            description = "Test repository for smoke test",
            defaultBranch = "main",
            createdTs = Instant.now(),
            ownerId = savedOwner.idNotNull,
            ownerType = savedOwner.type,
            ownerLogin = savedOwner.login,
            homepage = null,
            hasGhPages = false,
            hasIssues = true,
            hasWiki = true,
            hasReadme = true,
            licenseKey = "mit",
            licenseName = "MIT License",
            stars = 0,
            openIssues = 0,
            lastActivityTs = Instant.now(),
            updatedAtTs = Instant.now()
        )
        val savedRepo = scmRepositoryRepository.upsert(repoEntity)
        savedRepoId = savedRepo.idNotNull

        // Create a project with the repository
        val projectEntity = ProjectEntity(
            id = null,
            scmRepoId = savedRepo.idNotNull,
            ownerId = savedRepo.ownerId,
            name = savedRepo.name,
            description = "Test project for smoke test",
            minimizedReadme = null,
            latestVersion = "1.0.0",
            latestVersionTs = Instant.now(),
        )
        val savedProject = projectRepository.insert(projectEntity)
        savedProjectId = savedProject.id

        // Create a package with a unique name for easy identification in search results
        val packageDTO = PackageDTO(
            id = null,
            projectId = savedProject.id,
            repo = ScraperType.SEARCH_MAVEN,
            groupId = "io.test",
            artifactId = "unique-test-${System.currentTimeMillis()}",
            version = "1.0.0",
            releaseTs = Instant.now(),
            description = "A test package for smoke testing",
            url = "https://example.com/unique-test",
            scmUrl = "https://github.com/example/unique-test",
            buildTool = "gradle",
            buildToolVersion = "7.0.0",
            kotlinVersion = "1.8.0",
            developers = listOf(
                PackageDeveloper(
                    name = "Test Developer",
                    url = "https://github.com/testdev"
                )
            ),
            licenses = listOf(
                PackageLicense(
                    name = "Apache 2.0",
                    url = "https://www.apache.org/licenses/LICENSE-2.0"
                )
            ),
            configuration = Configuration(
                projectSettings = Configuration.ProjectSettings(
                    isHmppEnabled = true,
                    isCompatibilityMetadataVariantEnabled = false
                ),
                jvmPlatform = Configuration.JvmPlatform(
                    jvmTarget = "11",
                    withJavaEnabled = true
                ),
                androidJvmPlatform = null,
                nativePlatform = null,
                wasmPlatform = null,
                jsPlatform = null
            ),
            targets = listOf(PackageTarget(
                platform = PackagePlatform.JVM,
                target = "jvm"
            ))
        )

        val savedPackage = packageRepository.save(packageDTO.toEntity())
        savedPackageId = savedPackage.id

        // Refresh search index to make the project searchable
        searchService.refreshSearchViews()

        // Verify the project is searchable using the unique repository name
        val result = mockMvc.get("/search/projects") {
            param("query", uniqueRepoName)
            param("sort", "relevance")
        }.andExpect {
            status { isOk() }
        }.andReturn()

        val foundProjects: List<SearchProjectResultDTO> = objectMapper.readValue(
            result.response.contentAsString,
            objectMapper.typeFactory.constructCollectionType(List::class.java, SearchProjectResultDTO::class.java)
        )

        // Verify the project is found
        assertTrue(foundProjects.isNotEmpty(), "Project should be found before banning the package")

        // Find our project by repository name
        val ourProject = foundProjects.find { it.name == uniqueRepoName }
        assertTrue(ourProject != null, "Our project with name '$uniqueRepoName' should be found in search results")

        // Ban the package
        val banResult = blacklistService.banPackage(savedPackage.groupId, savedPackage.artifactId, "Test ban reason")
        assertTrue(banResult, "Package should be banned successfully")

        // Refresh search index to reflect the ban
        searchService.refreshSearchViews()

        // Verify the project is no longer searchable
        val resultAfterBan = mockMvc.get("/search/projects") {
            param("query", uniqueRepoName)
            param("sort", "relevance")
        }.andExpect {
            status { isOk() }
        }.andReturn()

        val foundProjectsAfterBan: List<SearchProjectResultDTO> = objectMapper.readValue(
            resultAfterBan.response.contentAsString,
            objectMapper.typeFactory.constructCollectionType(List::class.java, SearchProjectResultDTO::class.java)
        )

        // Verify our project is not found
        val ourProjectAfterBan = foundProjectsAfterBan.find { it.name == uniqueRepoName }
        assertTrue(
            ourProjectAfterBan == null,
            "Our project with name '$uniqueRepoName' should not be found after banning the package"
        )
    }
}
