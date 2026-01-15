package io.klibs.integration.github

import io.klibs.integration.github.model.ReadmeFetchResult
import io.micrometer.core.instrument.simple.SimpleMeterRegistry
import okhttp3.OkHttpClient
import org.kohsuke.github.GitHubBuilder
import java.time.Instant
import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

// TODO unignore and make it runnable under a profile/flag for IT. hits unauthorized requests limits otherwise
@Ignore
class GitHubIntegrationTest {

    private val meterRegistry = SimpleMeterRegistry()
    private val githubApi = GitHubBuilder().build()
    
    private val gitHubIntegration: GitHubIntegration = GitHubIntegrationKohsukeLibrary(
        meterRegistry,
        githubApi,
        OkHttpClient(),
        GitHubIntegrationProperties(cache = GitHubIntegrationProperties.Cache())
    )

    @Test
    fun `should get a valid GitHub repository`() {
        val dokkaRepository = gitHubIntegration.getRepository("kotlin", "dokka")
        assertNotNull(dokkaRepository)

        assertEquals("dokka", dokkaRepository.name)
        assertEquals("Kotlin", dokkaRepository.owner)
        assertEquals(Instant.ofEpochMilli(1405156522000), dokkaRepository.createdAt)
        assertEquals("API documentation engine for Kotlin", dokkaRepository.description)
        assertEquals("master", dokkaRepository.defaultBranch)
        assertEquals("https://kotl.in/dokka", dokkaRepository.homepage)
        assertEquals(false, dokkaRepository.hasWiki)
        assertEquals(true, dokkaRepository.hasGhPages)
        assertEquals(true, dokkaRepository.hasIssues)
        assertTrue("Expected more than 3k stars") {
            dokkaRepository.stars > 3000
        }
    }

    @Test
    fun `should get a valid license from the repository`() {
        val license = gitHubIntegration.getLicense(DOKKA_REPOSITORY_ID)
        assertNotNull(license)
        assertEquals("apache-2.0", license.key)
        assertEquals("Apache License 2.0", license.name)
    }

    @Test
    fun `should get README for the repository`() {
        val readmeResult = gitHubIntegration.getReadmeWithModifiedSinceCheck(DOKKA_REPOSITORY_ID)
        assertTrue(readmeResult is ReadmeFetchResult.Content)
        val dokkaReadmeMd = readmeResult.markdown
        assertTrue("Non matching README.md") {
            dokkaReadmeMd.startsWith("# Dokka")
        }

        val readmeHtml = assertNotNull(gitHubIntegration.markdownToHtml(dokkaReadmeMd, DOKKA_REPOSITORY_ID))
        assertTrue("Expected README to begin with <h1>Dokka</h1>") {
            readmeHtml.startsWith("<h1 dir=\"auto\">Dokka</h1>")
        }
    }

    @Test
    fun `should return null for a non-existing repo`() {
        val dokkaRepository = gitHubIntegration.getRepository("nonexistingowner", "somereponamethatdoesnotexist")
        assertNull(dokkaRepository)
    }

    @Test
    fun `should get an actual user`() {
        val user = gitHubIntegration.getUser("bnorm")
        assertNotNull(user)

        assertEquals("bnorm", user.login)
        assertEquals("User", user.type)

        assertEquals("Brian Norman", user.name)
        assertEquals("@JetBrains", user.company)
        assertEquals("blog.bnorm.dev", user.blog)
        assertEquals("Minneapolis, MN, USA", user.location)
        assertEquals(null, user.email)
        assertEquals("git commit -m \"Is this thing on?\"", user.bio)
        assertEquals("bnormcodes", user.twitterUsername)
        assertTrue { user.followers > 60 }
    }

    companion object {
        private const val DOKKA_REPOSITORY_ID = 21763603L // https://github.com/kotlin/dokka
    }
}
