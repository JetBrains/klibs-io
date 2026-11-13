package io.klibs.core.readme

import io.klibs.core.readme.impl.ReadmeMinimizationProcessor
import io.klibs.integration.github.GitHubIntegration
import org.springframework.stereotype.Service

@Service
class AndroidxReadmeContentBuilder(
    private val gitHubIntegration: GitHubIntegration,
    private val minimizer: ReadmeMinimizationProcessor,
) {
    fun buildFromMarkdown(readme: String, projectName: String): ReadmeContent {
        val renderedReadme = gitHubIntegration.markdownRenderRaw(readme)
            ?: error("Problem rendering markdown for androidx.$projectName")
        return ReadmeContent(
            raw = readme, markdown = renderedReadme, html = renderedReadme,
            minimized = minimizer.process(readme, "", "", ""),
        )
    }
}
