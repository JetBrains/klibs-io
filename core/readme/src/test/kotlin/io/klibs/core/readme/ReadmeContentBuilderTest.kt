package io.klibs.core.readme

import io.klibs.integration.github.GitHubIntegration
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import kotlin.test.assertEquals

class ReadmeContentBuilderTest {
    private val gitHubIntegration = mock<GitHubIntegration> {
        whenever(it.markdownToHtml(any(), any())).thenAnswer { invocation ->
            "<html>${invocation.getArgument<String>(0)}</html>"
        }
        whenever(it.markdownRender(any(), any())).thenAnswer { invocation ->
            invocation.getArgument<String>(0)
        }
    }
    private val builder = ReadmeContentBuilder(gitHubIntegration, emptyList())

    @ParameterizedTest
    @ValueSource(strings = ["\u0000", "\u0000# README", "# README\u0000broken", "# README\u0000"])
    fun `NUL-containing README is processed like empty README`(markdown: String) {
        assertEquals(build(""), build(markdown))
    }

    @ParameterizedTest
    @ValueSource(strings = ["", "# Hello 世界", "# Привет\n\nREADME", "literal \\u0000"])
    fun `valid README is preserved`(markdown: String) {
        assertEquals(
            GitHubIndexingReadmeContent(markdown, markdown, "<html>$markdown</html>", markdown),
            build(markdown),
        )
    }

    private fun build(markdown: String) = builder.buildFromMarkdown(markdown, 123L, "owner", "repo", "main")
}