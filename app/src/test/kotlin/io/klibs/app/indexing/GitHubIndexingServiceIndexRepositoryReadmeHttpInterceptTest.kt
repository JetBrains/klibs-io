package io.klibs.app.indexing

import BaseUnitWithDbLayerTest
import io.klibs.core.scm.repository.ScmRepositoryRepository
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.jupiter.api.Test
import io.klibs.integration.github.GitHubIntegration
import io.klibs.integration.github.model.GitHubLicense
import io.klibs.integration.github.model.GitHubRepository
import io.klibs.integration.github.model.GitHubUser
import jakarta.transaction.Transactional
import org.kohsuke.github.GitHub
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.boot.test.mock.mockito.SpyBean
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class GitHubIndexingServiceIndexRepositoryReadmeHttpInterceptTest : BaseUnitWithDbLayerTest() {

    @MockBean
    private lateinit var okHttpClient: OkHttpClient

    @Autowired
    private lateinit var uut: GitHubIndexingService

    @Autowired
    private lateinit var scmRepositoryRepository: ScmRepositoryRepository

    @SpyBean
    private lateinit var gitHubIntegration: GitHubIntegration

    @MockBean
    private lateinit var githubApi: GitHub

    @Test
    @Transactional
    fun `indexRepository fetches README via intercepted HTTP and persists it`() {
        // Configure @MockBean OkHttpClient to return README content for /readme calls
        val mockCall = mock<okhttp3.Call>()
        whenever(okHttpClient.newCall(any())).thenAnswer { invocation ->
            val request = invocation.arguments[0] as Request
            val response = if (request.url.encodedPath.endsWith("/readme")) {
                Response.Builder()
                    .request(request)
                    .protocol(Protocol.HTTP_1_1)
                    .code(200)
                    .message("OK")
                    .body("# Title\nSome content".toResponseBody(null))
                    .build()
            } else {
                Response.Builder()
                    .request(request)
                    .protocol(Protocol.HTTP_1_1)
                    .code(404)
                    .message("Not Found")
                    .body("".toResponseBody(null))
                    .build()
            }
            whenever(mockCall.execute()).thenReturn(response)
            mockCall
        }
        val ownerLogin = "k-libs"
        val repoName = "klibs-io"
        val repoId = 42_4242L

        // Stub the spy GitHubIntegration for repository/user/license only
        val ghRepoModel = GitHubRepository(
            nativeId = repoId,
            name = repoName,
            createdAt = Instant.parse("2024-01-01T00:00:00Z"),
            description = "Test repo",
            defaultBranch = "main",
            owner = ownerLogin,
            homepage = null,
            hasGhPages = false,
            hasIssues = true,
            hasWiki = false,
            stars = 10,
            openIssues = 1,
            lastActivity = Instant.parse("2024-01-02T00:00:00Z")
        )
        val ghUserModel = GitHubUser(
            id = 62517686L,
            login = ownerLogin,
            type = "Organization",
            name = ownerLogin,
            company = null,
            blog = null,
            location = null,
            email = null,
            bio = null,
            twitterUsername = null,
            followers = 0
        )
        val ghLicenseModel = GitHubLicense(
            key = "apache-2.0",
            name = "Apache License 2.0"
        )

        whenever(gitHubIntegration.getRepository(ownerLogin, repoName)).thenReturn(ghRepoModel)
        whenever(gitHubIntegration.getUser(ownerLogin)).thenReturn(ghUserModel)
        whenever(gitHubIntegration.getLicense(repoId)).thenReturn(ghLicenseModel)

        whenever(gitHubIntegration.markdownToHtml("# Title\nSome content", repoId)).thenReturn("<h1>Title</h1><p>Some content</p>")
        whenever(gitHubIntegration.markdownRender("# Title\nSome content", repoId)).thenReturn("# Title\nSome content")

        val persisted = uut.indexRepository(ownerLogin, repoName)
        assertNotNull(persisted)
        assertTrue(persisted.hasReadme, "hasReadme should be true when README content is returned")
        assertEquals(persisted.minimizedReadme?.isNotBlank(), true)

        val fromDb = scmRepositoryRepository.findByNativeId(repoId)
        assertNotNull(fromDb)
        assertTrue(fromDb.hasReadme)
    }
}
