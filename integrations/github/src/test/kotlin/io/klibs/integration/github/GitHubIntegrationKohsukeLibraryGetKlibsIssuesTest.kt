package io.klibs.integration.github

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import okhttp3.OkHttpClient
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertNull
import org.junit.jupiter.api.extension.ExtendWith
import org.kohsuke.github.*
import org.mockito.Answers
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.junit.jupiter.MockitoSettings
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.mockito.quality.Strictness
import java.io.FileNotFoundException
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.*

@ExtendWith(MockitoExtension::class)
@MockitoSettings(strictness = Strictness.LENIENT)
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

    @Mock(answer = Answers.RETURNS_SELF)
    private lateinit var issueQueryBuilder: GHIssueQueryBuilder.ForRepository

    @Mock
    private lateinit var pagedIterable: PagedIterable<GHIssue>

    private lateinit var library: GitHubIntegrationKohsukeLibrary

    private val klibsRepoName = "JetBrains/klibs-io"
    private val processedLabel = "triaged"
    private val batchSize = 5

    @BeforeEach
    fun setUp() {
        meterRegistry = SimpleMeterRegistry()

        whenever(githubApi.getRepository(klibsRepoName)).thenReturn(klibsRepo)
        whenever(klibsRepo.queryIssues()).thenReturn(issueQueryBuilder)
        whenever(issueQueryBuilder.list()).thenReturn(pagedIterable)

        library = GitHubIntegrationKohsukeLibrary(
            meterRegistry = meterRegistry,
            githubApi = githubApi,
            okHttpClient = okHttpClient,
            gitHubIntegrationProperties = gitHubIntegrationProperties,
            jsonMapper = jacksonObjectMapper(),
            klibsRepoName = klibsRepoName,
            processedLabel = processedLabel,
            batchSize = batchSize
        )
    }

    @Test
    fun `should return empty list when no issues are found`() {
        mockPagedIterable(emptyList())

        val result = library.getKlibsIssuesByLabel("index-request", Instant.now())

        assertTrue(result.issues.isEmpty())
        assertTrue(result.hasMore == false)
    }

    @Test
    fun `should filter out pull requests`() {
        val prIssue = mockIssue(isPullRequest = true, body = "PR Body")
        val normalIssue = mockIssue(isPullRequest = false, body = "Issue Body")

        mockPagedIterable(listOf(prIssue, normalIssue))

        val result = library.getKlibsIssuesByLabel("index-request", Instant.now())

        assertEquals(1, result.issues.size)
        assertEquals("Issue Body", result.issues[0].body)
        assertTrue(result.hasMore == false)
    }

    @Test
    fun `should filter out issues with triaged label`() {
        val triagedIssue =
            mockIssue(isPullRequest = false, body = "Triaged Issue", labelNames = listOf("index-request", "triaged"))
        val untriagedIssue =
            mockIssue(isPullRequest = false, body = "Untriaged Issue", labelNames = listOf("index-request"))

        mockPagedIterable(listOf(triagedIssue, untriagedIssue))

        val result = library.getKlibsIssuesByLabel("index-request", Instant.now())

        assertEquals(1, result.issues.size)
        assertEquals("Untriaged Issue", result.issues[0].body)
        assertTrue(result.hasMore == false)
    }

    @Test
    fun `should correctly map GHIssue to GitHubIssue`() {
        val since = Instant.now()
        val issue = mockIssue(
            number = 42,
            body = "Issue Body",
            isPullRequest = false,
            labelNames = listOf("index-request"),
            createdAt = Date.from(since)
        )

        mockPagedIterable(listOf(issue))

        val result = library.getKlibsIssuesByLabel("index-request", since)

        assertEquals(1, result.issues.size)
        val mappedIssue = result.issues[0]
        assertEquals(42, mappedIssue.number)
        assertEquals("Issue Body", mappedIssue.body)
        assertEquals(listOf("index-request"), mappedIssue.labels)
        assertEquals(since.truncatedTo(ChronoUnit.MILLIS), mappedIssue.createdAt)
        assertTrue(result.hasMore == false)
    }

    @Test
    fun `should return empty list when FileNotFoundException is thrown`() {
        whenever(pagedIterable.iterator()).thenAnswer { throw FileNotFoundException() }

        val result = library.getKlibsIssuesByLabel("index-request", Instant.now())

        assertTrue(result.issues.isEmpty())
        assertNull(result.hasMore)
    }

    @Test
    fun `should respect batch size and drop the last issue if limit is exceeded`() {
        val issues = (1..7).map { i ->
            mockIssue(number = i, body = "Issue $i", isPullRequest = false)
        }

        mockPagedIterable(issues)

        val result = library.getKlibsIssuesByLabel("index-request", Instant.now())

        assertEquals(batchSize, result.issues.size)
        assertEquals("Issue 1", result.issues.first().body)
        assertEquals("Issue 5", result.issues.last().body)
        assertTrue(result.hasMore == true)
    }

    private fun mockPagedIterable(issues: List<GHIssue>) {
        val listIterator = issues.iterator()
        val iteratorMock = mock<PagedIterator<GHIssue>> {
            on { hasNext() } doAnswer { listIterator.hasNext() }
            on { next() } doAnswer { listIterator.next() }
        }
        whenever(pagedIterable.iterator()).thenReturn(iteratorMock)
    }

    private fun mockIssue(
        number: Int = 1,
        body: String = "Body",
        isPullRequest: Boolean = false,
        labelNames: List<String> = emptyList(),
        createdAt: Date = Date()
    ): GHIssue {
        val mockedLabels = labelNames.map { name ->
            mock<GHLabel> { on { it.name } doReturn name }
        }

        val issue = mock<GHIssue> {
            on { it.number } doReturn number
            on { it.body } doReturn body
            on { it.isPullRequest } doReturn isPullRequest
            on { it.labels } doReturn mockedLabels
        }

        // We can't simply put it into the mock structure because Mockito clashes with Kohsuke's hidden bridge methods
        doAnswer { createdAt }.whenever(issue).getCreatedAt()

        return issue
    }
}