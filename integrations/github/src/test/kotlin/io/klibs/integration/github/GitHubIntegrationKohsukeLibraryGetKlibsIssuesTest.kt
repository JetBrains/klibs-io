package io.klibs.integration.github

import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import okhttp3.OkHttpClient
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.kohsuke.github.*
import org.mockito.Mock
import org.mockito.Mockito.lenient
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.io.FileNotFoundException
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.*

@ExtendWith(MockitoExtension::class)
class GitHubIntegrationKohsukeLibraryGetKlibsIssuesTest {

    @Mock
    private lateinit var meterRegistry: MeterRegistry

    @Mock
    private lateinit var githubApi: GitHub

    @Mock
    private lateinit var okHttpClient: OkHttpClient

    @Mock
    private lateinit var gitHubIntegrationProperties: GitHubIntegrationProperties

    @Mock
    private lateinit var klibsRepo: GHRepository

    @Mock
    private lateinit var issueQueryBuilder: GHIssueQueryBuilder.ForRepository

    @Mock
    private lateinit var pagedIterable: PagedIterable<GHIssue>

    private lateinit var library: GitHubIntegrationKohsukeLibrary

    private val klibsRepoName = "JetBrains/klibs-io"
    private val processedLabel = "triaged"

    @BeforeEach
    fun setUp() {
        meterRegistry = SimpleMeterRegistry()

        whenever(githubApi.getRepository(klibsRepoName)).thenReturn(klibsRepo)

        library = GitHubIntegrationKohsukeLibrary(
            meterRegistry = meterRegistry,
            githubApi = githubApi,
            okHttpClient = okHttpClient,
            gitHubIntegrationProperties = gitHubIntegrationProperties,
            klibsRepoName = klibsRepoName,
            processedLabel = processedLabel
        )
    }

    @Test
    fun `should return empty list when no issues are found`() {
        val since = Instant.now()
        setupIssueQueryMock(since)
        whenever(pagedIterable.toList()).thenReturn(emptyList())

        val result = library.getKlibsIssuesByLabel("index-request", since)

        assertTrue(result.isEmpty())
    }

    @Test
    fun `should filter out pull requests`() {
        val since = Instant.now()
        setupIssueQueryMock(since)

        val prIssue = mockIssue(isPullRequest = true, title = "PR Title")
        val normalIssue = mockIssue(isPullRequest = false, title = "Issue Title")

        whenever(pagedIterable.toList()).thenReturn(listOf(prIssue, normalIssue))

        val result = library.getKlibsIssuesByLabel("index-request", since)

        assertEquals(1, result.size)
        assertEquals("Issue Title", result[0].title)
    }

    @Test
    fun `should filter out issues with triaged label`() {
        val since = Instant.now()
        setupIssueQueryMock(since)

        val triagedIssue = mockIssue(isPullRequest = false, title = "Triaged Issue", labelNames = listOf("index-request", "triaged"))
        val untriagedIssue = mockIssue(isPullRequest = false, title = "Untriaged Issue", labelNames = listOf("index-request"))

        whenever(pagedIterable.toList()).thenReturn(listOf(triagedIssue, untriagedIssue))

        val result = library.getKlibsIssuesByLabel("index-request", since)

        assertEquals(1, result.size)
        assertEquals("Untriaged Issue", result[0].title)
    }

    @Test
    fun `should correctly map GHIssue to GitHubIssue`() {
        val since = Instant.now()
        setupIssueQueryMock(since)

        val issueDate = Date.from(since)
        val issue = mockIssue(
            number = 42,
            title = "Test Issue",
            body = "Issue Body",
            isPullRequest = false,
            labelNames = listOf("index-request"),
            updatedAt = issueDate
        )

        whenever(pagedIterable.toList()).thenReturn(listOf(issue))

        val result = library.getKlibsIssuesByLabel("index-request", since)

        assertEquals(1, result.size)
        val mappedIssue = result[0]
        assertEquals(42, mappedIssue.number)
        assertEquals("Test Issue", mappedIssue.title)
        assertEquals("Issue Body", mappedIssue.body)
        assertEquals(listOf("index-request"), mappedIssue.labels)
        assertEquals(since.truncatedTo(ChronoUnit.MILLIS), mappedIssue.updatedAt)
    }

    @Test
    fun `should return empty list when FileNotFoundException is thrown`() {
        val since = Instant.now()
        setupIssueQueryMock(since)
        whenever(pagedIterable.toList()).thenAnswer { throw FileNotFoundException() }

        val result = library.getKlibsIssuesByLabel("index-request", since)

        assertTrue(result.isEmpty())
    }


    private fun setupIssueQueryMock(since: Instant) {
        whenever(klibsRepo.queryIssues()).thenReturn(issueQueryBuilder)
        whenever(issueQueryBuilder.label(any())).thenReturn(issueQueryBuilder)
        whenever(issueQueryBuilder.state(GHIssueState.OPEN)).thenReturn(issueQueryBuilder)
        whenever(issueQueryBuilder.since(Date.from(since))).thenReturn(issueQueryBuilder)
        whenever(issueQueryBuilder.list()).thenReturn(pagedIterable)
    }

    private fun mockIssue(
        number: Int = 1,
        title: String = "Title",
        body: String = "Body",
        isPullRequest: Boolean = false,
        labelNames: List<String> = emptyList(),
        updatedAt: Date = Date()
    ): GHIssue {
        val issue = mock<GHIssue>()
        lenient().whenever(issue.number).thenReturn(number)
        lenient().whenever(issue.title).thenReturn(title)
        lenient().whenever(issue.body).thenReturn(body)
        lenient().whenever(issue.isPullRequest).thenReturn(isPullRequest)
        lenient().whenever(issue.updatedAt).thenReturn(updatedAt)

        val labels = labelNames.map { name ->
            val label = mock<GHLabel>()
            lenient().whenever(label.name).thenReturn(name)
            label
        }
        lenient().whenever(issue.labels).thenReturn(labels)
        return issue
    }

}