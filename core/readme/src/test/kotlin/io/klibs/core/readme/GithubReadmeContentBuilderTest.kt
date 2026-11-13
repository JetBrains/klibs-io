package io.klibs.core.readme

import io.klibs.integration.github.GitHubIntegration
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import kotlin.test.assertEquals

class GithubReadmeContentBuilderTest {
    private val gitHubIntegration = mockk<GitHubIntegration> {
        every { markdownToHtml(any(), any()) } answers { "<html>${firstArg<String>()}</html>" }
        every { markdownRender(any(), any()) } answers { firstArg() }
    }
    private val builder = GithubReadmeContentBuilder(gitHubIntegration, emptyList())

    @ParameterizedTest
    @ValueSource(strings = ["\u0000", "\u0000# README", "# README\u0000broken", "# README\u0000"])
    fun `NUL-containing README is processed like empty README`(markdown: String) {
        assertEquals(build(""), build(markdown))
    }

    @ParameterizedTest
    @ValueSource(strings = ["", "# Hello 世界", "# Привет\n\nREADME", "literal \\u0000"])
    fun `valid README is preserved`(markdown: String) {
        assertEquals(
            ReadmeContent(markdown, markdown, "<html>$markdown</html>", markdown),
            build(markdown),
        )
    }

    private fun build(markdown: String) = builder.buildFromMarkdown(markdown, 123L, "owner", "repo", "main")
}