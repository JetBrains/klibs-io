package io.klibs.core.project

import io.klibs.core.owner.ScmOwnerType
import org.junit.jupiter.api.Test
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ProjectDetailsLinkTest {

    private fun createDetails(
        ownerLogin: String = "someOwner",
        repoName: String = "someRepo",
        name: String = "someProject",
        hasGhPages: Boolean = false,
        hasReadme: Boolean = false,
        linkHomepage: String? = null,
    ): ProjectDetails {
        val now = Instant.now()
        return ProjectDetails(
            id = 1,
            ownerType = ScmOwnerType.ORGANIZATION,
            ownerLogin = ownerLogin,
            repoName = repoName,
            name = name,
            description = null,
            platforms = emptyList(),
            latestReleaseVersion = "1.0.0",
            latestReleasePublishedAt = now,
            linkHomepage = linkHomepage,
            hasGhPages = hasGhPages,
            hasIssues = false,
            hasWiki = false,
            hasReadme = hasReadme,
            stars = 0,
            createdAt = now,
            openIssues = null,
            licenseName = null,
            lastActivityAt = now,
            updatedAt = now,
            tags = emptyList(),
            markers = emptyList(),
        )
    }

    @Test
    fun `regular project github link points to owner and repo`() {
        val details = createDetails(ownerLogin = "Kotlin", repoName = "kotlinx.coroutines")
        assertEquals("https://github.com/Kotlin/kotlinx.coroutines", details.getGitHubRepositoryLink())
    }

    @Test
    fun `regular project github pages link returned when hasGhPages is true`() {
        val details = createDetails(ownerLogin = "Kotlin", repoName = "dokka", hasGhPages = true)
        assertEquals("https://Kotlin.github.io/dokka", details.getGitHubPagesLink())
    }

    @Test
    fun `regular project github pages link is null when hasGhPages is false`() {
        val details = createDetails(ownerLogin = "Kotlin", repoName = "dokka", hasGhPages = false)
        assertNull(details.getGitHubPagesLink())
    }

    @Test
    fun `regular project homepage returns linkHomepage`() {
        val details = createDetails(linkHomepage = "https://example.com")
        assertEquals("https://example.com", details.getHomepageLink())
    }

    @Test
    fun `regular project homepage returns null when linkHomepage is null`() {
        val details = createDetails(linkHomepage = null)
        assertNull(details.getHomepageLink())
    }

    // --- AndroidX ---

    @Test
    fun `androidx project github link points to subfolder in monorepo`() {
        val details = createDetails(ownerLogin = "androidx", repoName = "androidx", name = "annotation")
        assertEquals(
            "https://github.com/androidx/androidx/tree/androidx-main/annotation",
            details.getGitHubRepositoryLink()
        )
    }

    @Test
    fun `androidx room project github link points to room3 subfolder in monorepo`() {
        val details = createDetails(ownerLogin = "androidx", repoName = "androidx", name = "room")
        assertEquals(
            "https://github.com/androidx/androidx/tree/androidx-main/room3",
            details.getGitHubRepositoryLink()
        )
    }

    @Test
    fun `androidx project github pages link is always null`() {
        val details = createDetails(ownerLogin = "androidx", repoName = "androidx", name = "annotation", hasGhPages = true)
        assertNull(details.getGitHubPagesLink())
    }

    @Test
    fun `androidx project with readme has developer android homepage`() {
        val details = createDetails(ownerLogin = "androidx", repoName = "androidx", name = "annotation", hasReadme = true)
        assertEquals(
            "https://developer.android.com/jetpack/androidx/releases/annotation",
            details.getHomepageLink()
        )
    }
}
