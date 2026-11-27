package io.klibs.core.readme

import io.klibs.core.readme.impl.ReadmeMinimizationProcessor
import io.klibs.core.readme.service.AndroidxReadmeContentBuilder
import io.klibs.integration.github.GitHubIntegration
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class AndroidxReadmeContentBuilderTest {
    private val gitHubIntegration = mockk<GitHubIntegration> {
        every { markdownRenderRaw(any()) } answers { firstArg() }
    }
    private val minimizer = mockk<ReadmeMinimizationProcessor> {
        every { process(any(), any(), any(), any()) } answers { firstArg() }
    }
    private val uut = AndroidxReadmeContentBuilder(gitHubIntegration, minimizer)

    @Test
    fun `feedback section and everything after it are replaced with an issue tracker section`() {
        val readme = """
            # Paging

            Load data in pages.

            ## Feedback

            [Create a new issue](https://issuetracker.google.com/issues/new?component=413106&template=1096385)

            Your feedback helps make Jetpack better. Please take a look at the
            [existing issues](https://issuetracker.google.com/issues?q=componentid:413106+status:open)
            in this library before you create a new one.
        """.trimIndent()

        assertEquals(
            """
            # Paging

            Load data in pages.

            ## Issue tracker

            [Issue Tracker](https://issuetracker.google.com/issues?q=componentid:413106+status:open)
            """.trimIndent(),
            clean(readme),
        )
    }

    @Test
    fun `readme without feedback section is kept unchanged`() {
        val readme = """
            # Paging

            Load data in pages.

            ## Declaring dependencies

            Add the dependencies for the artifacts you need.
        """.trimIndent()

        assertEquals(readme, clean(readme))
    }

    @Test
    fun `feedback section without issue tracker link is cut without appending anything`() {
        val readme = """
            # Paging

            Load data in pages.

            ## Feedback

            Your feedback helps make Jetpack better.
        """.trimIndent()

        assertEquals(
            """
            # Paging

            Load data in pages.
            """.trimIndent(),
            clean(readme),
        )
    }

    @Test
    fun `feedback section with only a new issue link is cut without appending anything`() {
        val readme = """
            # Paging

            Load data in pages.

            ## Feedback

            [Create a new issue](https://issuetracker.google.com/issues/new?component=413106&template=1096385)
        """.trimIndent()

        assertEquals(
            """
            # Paging

            Load data in pages.
            """.trimIndent(),
            clean(readme),
        )
    }

    @Test
    fun `version table is removed together with its lead-in and last-updated lines`() {
        val readme = """
            # Annotation

            Expose metadata that helps tools understand your app's code.

            This table lists all the artifacts in the `androidx.annotation` group.

            | Artifact | Stable Release | Release Candidate | Beta Release | Alpha Release |
            |---|---|---|---|---|
            | annotation | [1.11.0](https://developer.android.com/jetpack/androidx/releases/annotation#1.11.0) | - | - | - |

            This library was last updated on: September 23, 2026

            ## Declaring dependencies

            Add the dependencies for the artifacts you need.
        """.trimIndent()

        assertEquals(
            """
            # Annotation

            Expose metadata that helps tools understand your app's code.




            ## Declaring dependencies

            Add the dependencies for the artifacts you need.
            """.trimIndent(),
            clean(readme),
        )
    }

    @Test
    fun `heading emptied by version table removal is removed while other tables are kept`() {
        val readme = """
            # Compose

            This table explains the groups and links to each set of release notes.

            | Group | Description |
            |---|---|
            | [compose.animation](https://developer.android.com/jetpack/androidx/releases/compose-animation.md.txt) | Build animations. |

            ## Versions

            This table lists the current versions of each group.

            | Maven Group ID | Latest Update | Stable Release | Release Candidate | Beta Release | Alpha Release |
            |---|---|---|---|---|---|
            | [compose.animation](https://developer.android.com/jetpack/androidx/releases/compose-animation.md.txt) | November 18, 2026 | [1.13.0](https://developer.android.com/jetpack/androidx/releases/compose-animation.md.txt#1.13.0) | - | - | - |

            ## Declaring dependencies

            Add the dependencies for the artifacts you need.
        """.trimIndent()

        assertEquals(
            """
            # Compose

            This table explains the groups and links to each set of release notes.

            | Group | Description |
            |---|---|
            | [compose.animation](https://developer.android.com/jetpack/androidx/releases/compose-animation) | Build animations. |

            ## Declaring dependencies

            Add the dependencies for the artifacts you need.
            """.trimIndent(),
            clean(readme),
        )
    }

    @Test
    fun `documentation index line is removed and md txt link suffixes are stripped`() {
        val readme = """
            > Documentation index: https://developer.android.com/jetpack/androidx/llms.txt

            # Sqlite

            [User Guide](https://developer.android.com/training/data-storage/sqlite.md.txt) Low-level SQLite APIs.
            See [Room](https://developer.android.com/jetpack/androidx/releases/room3.md.txt#3.0.3) as well.
        """.trimIndent()

        assertEquals(
            """

            # Sqlite

            [User Guide](https://developer.android.com/training/data-storage/sqlite) Low-level SQLite APIs.
            See [Room](https://developer.android.com/jetpack/androidx/releases/room3#3.0.3) as well.
            """.trimIndent(),
            clean(readme),
        )
    }

    @Test
    fun `raw readme is kept uncleaned while markdown and html contain the rendered cleaned readme`() {
        every { gitHubIntegration.markdownRenderRaw(any()) } answers { "<rendered>${firstArg<String>()}</rendered>" }
        val readme = "# Paging\n\n## Feedback\n\nFeedback text."

        val content = uut.buildFromMarkdown(readme, "androidx.paging")

        assertEquals(readme, content.raw)
        assertEquals("<rendered># Paging</rendered>", content.markdown)
        assertEquals(content.markdown, content.html)
        assertEquals("# Paging", content.minimized)
    }

    private fun clean(readme: String) = uut.buildFromMarkdown(readme, "androidx.test-project").markdown
}
