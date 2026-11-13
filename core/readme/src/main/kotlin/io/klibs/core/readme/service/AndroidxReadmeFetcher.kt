package io.klibs.core.readme.service

import io.klibs.integration.github.model.ReadmeFetchResult
import okhttp3.OkHttpClient
import okhttp3.Request
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service

@Service
class AndroidxReadmeFetcher(
    @param:Qualifier("androidxDocsHttpClient")
    private val okHttpClient: OkHttpClient,
) {
    fun getReadme(projectName: String): ReadmeFetchResult {
        val request = Request.Builder()
            .url("$BASE_URL${projectName.lowercase()}.md.txt")
            .build()

        return okHttpClient.newCall(request).execute().use { response ->
            val contentType = response.body?.contentType()
            val isMarkdown = contentType?.let { it.type == "text" && it.subtype == "markdown" } == true

            when {
                !response.isSuccessful -> ReadmeFetchResult.Error(status = response.code, message = response.message)
                !isMarkdown -> ReadmeFetchResult.Error(
                    status = response.code,
                    message = "Unexpected content type: $contentType"
                )

                else -> response.body?.string()
                    ?.takeIf { it.isNotBlank() }
                    ?.let { ReadmeFetchResult.Content(it) }
                    ?: ReadmeFetchResult.Error(status = response.code, message = "Empty readme")
            }
        }
    }

    private companion object {
        private const val BASE_URL = "https://developer.android.com/jetpack/androidx/releases/"
    }
}
