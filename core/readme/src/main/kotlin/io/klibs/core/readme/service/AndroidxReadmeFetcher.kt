package io.klibs.core.readme.service

import io.klibs.core.project.ANDROIDX_RELEASES_URL
import io.klibs.integration.github.model.ReadmeFetchResult
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.springframework.stereotype.Service

@Service
class AndroidxReadmeFetcher(
    private val androidxDocsHttpClient: OkHttpClient,
) {
    fun getReadme(projectName: String): ReadmeFetchResult {
        val request = buildReadmeRequest(projectName)
        return androidxDocsHttpClient.newCall(request).execute().use { it.toFetchResult() }
    }

    private fun Response.toFetchResult(): ReadmeFetchResult {
        val contentType = body?.contentType()
        val isMarkdown = contentType?.type == EXPECTED_CONTENT_TYPE && contentType.subtype == EXPECTED_CONTENT_SUBTYPE
        val markdown = body?.string()

        return when {
            !isSuccessful -> toError(message)
            !isMarkdown -> toError("Unexpected content type: $contentType")
            markdown.isNullOrBlank() -> toError("Empty readme")
            else -> ReadmeFetchResult.Content(markdown)
        }
    }

    private fun Response.toError(message: String) = ReadmeFetchResult.Error(status = code, message = message)

    private fun buildReadmeRequest(projectName: String): Request = Request.Builder()
        .url("$ANDROIDX_RELEASES_URL${projectName.lowercase()}.md.txt")
        .build()

    private companion object {
        private const val EXPECTED_CONTENT_TYPE = "text"
        private const val EXPECTED_CONTENT_SUBTYPE = "markdown"
    }
}
