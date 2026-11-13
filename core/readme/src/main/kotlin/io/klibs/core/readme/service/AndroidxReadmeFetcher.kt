package io.klibs.core.readme.service

import io.klibs.integration.github.model.ReadmeFetchResult
import okhttp3.OkHttpClient
import okhttp3.Request
import org.springframework.stereotype.Service

@Service
class AndroidxReadmeFetcher(
    val okHttpClient: OkHttpClient
) {
    fun getReadme(repositoryName: String): ReadmeFetchResult {
        val request = Request.Builder()
            .url(BASE_URL + repositoryName.lowercase() + ".md.txt")
            .header("Content-Type", "text/markdown")
            .get()
            .build()

        okHttpClient.newCall(request).execute().use { response ->
            if (response.isSuccessful){
                val readme = response.body?.string()
                if (response.headers["content-type"]?.contains("text/markdown") == true && (readme.isNullOrBlank().not())
                ){
                    return ReadmeFetchResult.Content(readme)
                }
                return ReadmeFetchResult.Error(message = "Invalid content type")
            } else {
                return ReadmeFetchResult.Error(status = response.code, message = response.message)
            }
        }
    }

    private companion object {
        private const val BASE_URL = "https://developer.android.com/jetpack/androidx/releases/"
    }
}
