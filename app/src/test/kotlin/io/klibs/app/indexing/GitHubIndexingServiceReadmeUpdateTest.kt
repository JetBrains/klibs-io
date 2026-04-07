package io.klibs.app.indexing

import BaseUnitWithDbLayerTest
import io.klibs.core.readme.repository.ReadmeMetadataRepository
import io.klibs.core.readme.service.S3ReadmeCRUDService
import io.klibs.core.scm.repository.ScmRepositoryRepository
import io.klibs.integration.github.GitHubIntegration
import io.klibs.integration.github.model.GitHubRepository
import io.klibs.integration.github.model.ReadmeFetchResult
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.context.jdbc.Sql
import java.time.Instant

class GitHubIndexingServiceReadmeUpdateTest : BaseUnitWithDbLayerTest() {

    @Autowired
    private lateinit var uut: GitHubIndexingService

    @Autowired
    private lateinit var readmeMetadataRepository: ReadmeMetadataRepository

    @Autowired
    private lateinit var scmRepositoryRepository: ScmRepositoryRepository

    @MockitoBean
    private lateinit var gitHubIntegration: GitHubIntegration

    @MockitoBean
    private lateinit var s3ReadmeService: S3ReadmeCRUDService

    @Test
    @Sql(scripts = ["classpath:sql/GitHubIndexingServiceReadmeUpdateTest/insert-readme-for-reprocessing.sql"])
    fun `updateReadme should reprocess when NotModified but lastProcessedAt is older than 7 days`() {
        val repoId = 368
        val projectId = 10001
        val repo = scmRepositoryRepository.findById(repoId)!!
        val readmeMetadataBefore = readmeMetadataRepository.findByScmRepoId(repoId)!!
        val ghRepo = GitHubRepository(
            nativeId = repo.nativeId,
            name = repo.name,
            owner = repo.ownerLogin,
            defaultBranch = repo.defaultBranch,
            stars = repo.stars,
            lastActivity = repo.lastActivityTs,
            createdAt = repo.createdTs,
            hasGhPages = repo.hasGhPages,
            hasIssues = repo.hasIssues,
            hasWiki = repo.hasWiki
        )

        whenever(gitHubIntegration.getRepository(repo.nativeId)).thenReturn(ghRepo)
        whenever(gitHubIntegration.getReadmeWithModifiedSinceCheck(any(), any()))
            .thenReturn(ReadmeFetchResult.NotModified)
        whenever(s3ReadmeService.readReadmeRaw(projectId, repoId))
            .thenReturn("# Old Content")
        whenever(gitHubIntegration.markdownToHtml(any(), any())).thenReturn("<html></html>")
        whenever(gitHubIntegration.markdownRender(any(), any())).thenReturn("Rendered")

        uut.updateRepo(repo)

        verify(s3ReadmeService).readReadmeRaw(projectId, repo.idNotNull)

        val readmeMetadataAfter = readmeMetadataRepository.findByScmRepoId(repo.idNotNull)!!
        assert(readmeMetadataAfter.lastProcessedAt.isAfter(readmeMetadataBefore.lastProcessedAt))
        assert(readmeMetadataAfter.lastSyncedAt.isAfter(readmeMetadataBefore.lastSyncedAt))
    }

    @Test
    @Sql(scripts = ["classpath:sql/GitHubIndexingServiceReadmeUpdateTest/insert-readme-not-for-reprocessing.sql"])
    fun `updateReadme should only update lastSyncedAt when NotModified and recently processed`() {
        val repoId = 368
        val projectId = 10001
        val repo = scmRepositoryRepository.findById(repoId)!!
        val readmeMetadataBefore = readmeMetadataRepository.findByScmRepoId(repoId)!!
        val ghRepo = GitHubRepository(
            nativeId = repo.nativeId,
            name = repo.name,
            owner = repo.ownerLogin,
            defaultBranch = repo.defaultBranch,
            stars = repo.stars,
            lastActivity = repo.lastActivityTs,
            createdAt = repo.createdTs,
            hasGhPages = repo.hasGhPages,
            hasIssues = repo.hasIssues,
            hasWiki = repo.hasWiki
        )

        whenever(gitHubIntegration.getRepository(repo.nativeId)).thenReturn(ghRepo)
        whenever(gitHubIntegration.getReadmeWithModifiedSinceCheck(any(), any()))
            .thenReturn(ReadmeFetchResult.NotModified)
        whenever(s3ReadmeService.readReadmeRaw(projectId, repoId))
            .thenReturn("# Old Content")

        uut.updateRepo(repo)

        verify(s3ReadmeService, never()).readReadmeRaw(projectId, repo.idNotNull)

        val readmeMetadataAfter = readmeMetadataRepository.findByScmRepoId(repo.idNotNull)!!
        assert(readmeMetadataAfter.lastProcessedAt == readmeMetadataBefore.lastProcessedAt)
        assert(readmeMetadataAfter.lastSyncedAt.isAfter(readmeMetadataBefore.lastSyncedAt))
    }

    @Test
    @Sql(scripts = ["classpath:sql/GitHubIndexingServiceReadmeUpdateTest/insert-readme-not-for-reprocessing.sql"])
    fun `updateReadme should update readme metadata when content of readme changed`() {
        val repoId = 368
        val projectId = 10001
        val repo = scmRepositoryRepository.findById(repoId)!!
        val readmeMetadataBefore = readmeMetadataRepository.findByScmRepoId(repoId)!!
        val ghRepo = GitHubRepository(
            nativeId = repo.nativeId,
            name = repo.name,
            owner = repo.ownerLogin,
            defaultBranch = repo.defaultBranch,
            stars = repo.stars,
            lastActivity = repo.lastActivityTs,
            createdAt = repo.createdTs,
            hasGhPages = repo.hasGhPages,
            hasIssues = repo.hasIssues,
            hasWiki = repo.hasWiki
        )

        whenever(gitHubIntegration.getRepository(repo.nativeId)).thenReturn(ghRepo)
        whenever(gitHubIntegration.getReadmeWithModifiedSinceCheck(any(), any()))
            .thenReturn(ReadmeFetchResult.Content("New Content"))
        whenever(s3ReadmeService.readReadmeRaw(projectId, repoId))
            .thenReturn("# Old Content")
        whenever(gitHubIntegration.markdownToHtml(any(), any())).thenReturn("<html></html>")
        whenever(gitHubIntegration.markdownRender(any(), any())).thenReturn("Rendered")

        uut.updateRepo(repo)

        verify(s3ReadmeService, never()).readReadmeRaw(projectId, repo.idNotNull)
        verify(gitHubIntegration).markdownToHtml(any(), any())

        val readmeMetadataAfter = readmeMetadataRepository.findByScmRepoId(repo.idNotNull)!!
        assert(readmeMetadataAfter.lastSyncedAt.isAfter(readmeMetadataBefore.lastSyncedAt))
        assert(readmeMetadataAfter.lastProcessedAt.isAfter(readmeMetadataBefore.lastProcessedAt))
    }

    @Test
    @Sql(scripts = ["classpath:sql/GitHubIndexingServiceReadmeUpdateTest/insert-repo-without-readme.sql"])
    fun `updateReadme should create readme metadata when no readme metadata`() {
        val repoId = 368
        val projectId = 10001
        val repo = scmRepositoryRepository.findById(repoId)!!
        val readmeMetadataBefore = readmeMetadataRepository.findByScmRepoId(repoId)
        assert(readmeMetadataBefore == null)

        val ghRepo = GitHubRepository(
            nativeId = repo.nativeId,
            name = repo.name,
            owner = repo.ownerLogin,
            defaultBranch = repo.defaultBranch,
            stars = repo.stars,
            lastActivity = repo.lastActivityTs,
            createdAt = repo.createdTs,
            hasGhPages = repo.hasGhPages,
            hasIssues = repo.hasIssues,
            hasWiki = repo.hasWiki
        )

        whenever(gitHubIntegration.getRepository(repo.nativeId)).thenReturn(ghRepo)
        whenever(gitHubIntegration.getReadmeWithModifiedSinceCheck(any(), any()))
            .thenReturn(ReadmeFetchResult.Content("New Content"))
        whenever(gitHubIntegration.markdownToHtml(any(), any())).thenReturn("<html></html>")
        whenever(gitHubIntegration.markdownRender(any(), any())).thenReturn("Rendered")

        uut.updateRepo(repo)

        verify(s3ReadmeService, never()).readReadmeRaw(projectId, repo.idNotNull)
        verify(gitHubIntegration).markdownToHtml(any(), any())

        val readmeMetadataAfter = readmeMetadataRepository.findByScmRepoId(repo.idNotNull)
        assert(readmeMetadataAfter != null)
        assert(readmeMetadataAfter!!.scmRepoId == repoId)
        assert(readmeMetadataAfter.lastSyncedAt.isAfter(Instant.EPOCH))
        assert(readmeMetadataAfter.lastProcessedAt.isAfter(Instant.EPOCH))
    }
}