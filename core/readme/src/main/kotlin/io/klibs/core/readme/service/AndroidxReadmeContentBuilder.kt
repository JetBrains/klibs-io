package io.klibs.core.readme.service

import io.klibs.core.readme.dto.ReadmeContent
import io.klibs.core.readme.impl.ReadmeMinimizationProcessor
import io.klibs.integration.github.GitHubIntegration
import org.springframework.stereotype.Service

@Service
class AndroidxReadmeContentBuilder(
    private val gitHubIntegration: GitHubIntegration,
    private val minimizer: ReadmeMinimizationProcessor,
) {
    fun buildFromMarkdown(readme: String, projectName: String): ReadmeContent {
        val cleanedReadme = cleanReadme(readme)
        val renderedReadme = gitHubIntegration.markdownRenderRaw(cleanedReadme)
            ?: error("Problem rendering markdown for $projectName")
        // Empty owner/repo/branch: the readme comes from developer.android.com, so there is no GitHub repo context,
        // and the ReadmeMinimizationProcessor does not use them anyway
        val minimizedReadme = minimizer.process(cleanedReadme, "", "", "")

        return ReadmeContent(
            raw = readme, markdown = renderedReadme, html = renderedReadme,
            minimized = minimizedReadme,
        )
    }

    private fun cleanReadme(readme: String): String =
        readme.lines()
            .filterNot { it.startsWith(DOCUMENTATION_INDEX_PREFIX) }
            .filterNot { it.startsWith(LAST_UPDATED_PREFIX) }
            .replaceFeedbackWithIssueTracker()
            .removeVersionTables()
            .joinToString("\n")
            .replace(MD_TXT_LINK_REGEX, "$1")

    private fun List<String>.replaceFeedbackWithIssueTracker(): List<String> {
        val feedbackIndex = indexOfFirst { it.trim() == FEEDBACK_HEADING }
        if (feedbackIndex == -1) return this

        val url = drop(feedbackIndex + 1)
            .takeWhile { !it.startsWith("## ") }
            .firstNotNullOfOrNull { ISSUE_LIST_LINK_REGEX.find(it)?.groupValues?.get(1) }

        val precedingLines = take(feedbackIndex).dropLastWhile(String::isBlank)

        return if (url != null) {
            precedingLines + listOf("", "## Issue tracker", "", "[Issue Tracker]($url)")
        } else {
            precedingLines
        }
    }

    private fun List<String>.removeVersionTables(): List<String> {
        val removedIndices = mutableSetOf<Int>()
        var index = 0

        while (index < size) {
            if (isVersionTableStart(index)) {
                val leadInIndex = tableLeadInIndex(index)
                if (leadInIndex != null) {
                    removedIndices.add(leadInIndex)
                }
                while (index < size && this[index].isTableLine()) {
                    removedIndices.add(index++)
                }
            } else {
                index++
            }
        }

        val allRemovedIndices = removedIndices + emptiedHeadingSections(removedIndices)
        return filterIndexed { i, _ -> i !in allRemovedIndices }
    }

    private fun String.isTableLine(): Boolean = trimStart().startsWith("|")

    private fun List<String>.isVersionTableStart(index: Int): Boolean =
        this[index].isTableLine() &&
                VERSION_TABLE_COLUMN in this[index] &&
                getOrNull(index - 1)?.isTableLine() != true

    private fun List<String>.tableLeadInIndex(tableStart: Int): Int? =
        (tableStart - 1 downTo 0)
            .firstOrNull { this[it].isNotBlank() }
            ?.takeIf { this[it].startsWith(TABLE_LEAD_IN_PREFIX) }

    private fun List<String>.emptiedHeadingSections(removed: Set<Int>): Set<Int> =
        (indices.filter { this[it].startsWith("#") } + size)
            .zipWithNext { heading, nextHeading -> heading until nextHeading }
            .filter { headingWithSection ->
                val section = headingWithSection.drop(1)
                section.any { it in removed } && section.all { it in removed || this[it].isBlank() }
            }
            .flatten()
            .toSet()

    private companion object {
        private const val DOCUMENTATION_INDEX_PREFIX = "> Documentation index:"
        private const val FEEDBACK_HEADING = "## Feedback"
        private const val LAST_UPDATED_PREFIX = "This library was last updated on"
        private const val TABLE_LEAD_IN_PREFIX = "This table lists"
        private const val VERSION_TABLE_COLUMN = "Stable Release"
        private val ISSUE_LIST_LINK_REGEX =
            Regex("""\((https://issuetracker\.google\.com/issues\?q=componentid:[^)]+)\)""")
        private val MD_TXT_LINK_REGEX = Regex("""(https://developer\.android\.com/[^)\s]*?)\.md\.txt""")
    }
}
