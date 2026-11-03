package io.klibs.app.indexing

import BaseUnitWithDbLayerTest
import com.ninjasquad.springmockk.MockkBean
import io.klibs.core.readme.repository.ReadmeMetadataRepository
import io.klibs.core.readme.service.S3ReadmeCRUDService
import io.klibs.core.scm.repository.ScmRepositoryRepository
import io.klibs.integration.github.GitHubIntegration
import io.klibs.integration.github.model.GitHubRepository
import io.klibs.integration.github.model.ReadmeFetchResult
import io.mockk.every
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.jdbc.Sql
import java.time.Instant

class GitHubIndexingServiceReadmeUpdateTest : BaseUnitWithDbLayerTest() {

    @Autowired
    private lateinit var uut: GitHubIndexingService

    @Autowired
    private lateinit var readmeMetadataRepository: ReadmeMetadataRepository

    @Autowired
    private lateinit var scmRepositoryRepository: ScmRepositoryRepository

    @MockkBean(relaxed = true)
    private lateinit var gitHubIntegration: GitHubIntegration

    @MockkBean(relaxUnitFun = true)
    private lateinit var s3ReadmeService: S3ReadmeCRUDService

    @Test
    @Sql(scripts = ["classpath:/sql/GitHubIndexingServiceReadmeUpdateTest/insert-readme-for-reprocessing.sql"])
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
            hasWiki = repo.hasWiki,
            archived = repo.archived
        )

        every { gitHubIntegration.getRepository(repo.nativeId) } returns ghRepo
        every { gitHubIntegration.getReadmeWithModifiedSinceCheck(any(), any()) } returns ReadmeFetchResult.NotModified
        every { s3ReadmeService.readReadmeRaw(projectId, repoId) } returns "# Old Content"
        every { gitHubIntegration.markdownToHtml(any(), any()) } returns "<html></html>"
        every { gitHubIntegration.markdownRender(any(), any()) } returns "Rendered"

        uut.updateRepo(repo)

        verify(exactly = 1) { s3ReadmeService.readReadmeRaw(projectId, repo.idNotNull) }

        val readmeMetadataAfter = readmeMetadataRepository.findByScmRepoId(repo.idNotNull)!!
        assert(readmeMetadataAfter.lastProcessedAt.isAfter(readmeMetadataBefore.lastProcessedAt))
        assert(readmeMetadataAfter.lastSyncedAt.isAfter(readmeMetadataBefore.lastSyncedAt))
    }

    @Test
    @Sql(scripts = ["classpath:/sql/GitHubIndexingServiceReadmeUpdateTest/insert-readme-not-for-reprocessing.sql"])
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
            hasWiki = repo.hasWiki,
            archived = repo.archived
        )

        every { gitHubIntegration.getRepository(repo.nativeId) } returns ghRepo
        every { gitHubIntegration.getReadmeWithModifiedSinceCheck(any(), any()) } returns ReadmeFetchResult.NotModified
        every { s3ReadmeService.readReadmeRaw(projectId, repoId) } returns "# Old Content"

        uut.updateRepo(repo)

        verify(exactly = 0) { s3ReadmeService.readReadmeRaw(projectId, repo.idNotNull) }

        val readmeMetadataAfter = readmeMetadataRepository.findByScmRepoId(repo.idNotNull)!!
        assert(readmeMetadataAfter.lastProcessedAt == readmeMetadataBefore.lastProcessedAt)
        assert(readmeMetadataAfter.lastSyncedAt.isAfter(readmeMetadataBefore.lastSyncedAt))
    }

    @Test
    @Sql(scripts = ["classpath:/sql/GitHubIndexingServiceReadmeUpdateTest/insert-readme-not-for-reprocessing.sql"])
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
            hasWiki = repo.hasWiki,
            archived = repo.archived
        )

        every { gitHubIntegration.getRepository(repo.nativeId) } returns ghRepo
        every { gitHubIntegration.getReadmeWithModifiedSinceCheck(any(), any()) } returns
            ReadmeFetchResult.Content("New Content")
        every { s3ReadmeService.readReadmeRaw(projectId, repoId) } returns "# Old Content"
        every { gitHubIntegration.markdownToHtml(any(), any()) } returns "<html></html>"
        every { gitHubIntegration.markdownRender(any(), any()) } returns "Rendered"

        uut.updateRepo(repo)

        verify(exactly = 0) { s3ReadmeService.readReadmeRaw(projectId, repo.idNotNull) }
        verify(exactly = 1) { gitHubIntegration.markdownToHtml(any(), any()) }

        val readmeMetadataAfter = readmeMetadataRepository.findByScmRepoId(repo.idNotNull)!!
        assert(readmeMetadataAfter.lastSyncedAt.isAfter(readmeMetadataBefore.lastSyncedAt))
        assert(readmeMetadataAfter.lastProcessedAt.isAfter(readmeMetadataBefore.lastProcessedAt))
    }

    @Test
    @Sql(scripts = ["classpath:/sql/GitHubIndexingServiceReadmeUpdateTest/insert-repo-without-readme.sql"])
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
            hasWiki = repo.hasWiki,
            archived = repo.archived
        )

        every { gitHubIntegration.getRepository(repo.nativeId) } returns ghRepo
        every { gitHubIntegration.getReadmeWithModifiedSinceCheck(any(), any()) } returns
            ReadmeFetchResult.Content("New Content")
        every { gitHubIntegration.markdownToHtml(any(), any()) } returns "<html></html>"
        every { gitHubIntegration.markdownRender(any(), any()) } returns "Rendered"

        uut.updateRepo(repo)

        verify(exactly = 0) { s3ReadmeService.readReadmeRaw(projectId, repo.idNotNull) }
        verify(exactly = 1) { gitHubIntegration.markdownToHtml(any(), any()) }

        val readmeMetadataAfter = readmeMetadataRepository.findByScmRepoId(repo.idNotNull)
        assert(readmeMetadataAfter != null)
        assert(readmeMetadataAfter!!.scmRepoId == repoId)
        assert(readmeMetadataAfter.lastSyncedAt.isAfter(Instant.EPOCH))
        assert(readmeMetadataAfter.lastProcessedAt.isAfter(Instant.EPOCH))
    }
}