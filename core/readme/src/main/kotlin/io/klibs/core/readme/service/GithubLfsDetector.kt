package io.klibs.core.readme.service

import okhttp3.OkHttpClient
import okhttp3.Request
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class GithubLfsDetector(
    private val okHttpClient: OkHttpClient
) {

    fun isLfsFile(rawUrl: String): Boolean {
        return try {
            val headRequest = Request.Builder()
                .url(rawUrl)
                .head()
                .build()

            val shouldSendGet = okHttpClient.newCall(headRequest).execute().use { response ->
                val contentTypeMatches = response.header("Content-Type")
                    ?.startsWith(LFS_CONTENT_TYPE)
                    ?: true
                val contentLengthMatches = response.header("Content-Length")
                    ?.toLongOrNull()
                    ?.let { it <= LFS_MAX_CONTENT_LENGTH_BYTES }
                    ?: true

                contentTypeMatches && contentLengthMatches
            }

            if (!shouldSendGet) {
                return false
            }

            val getRequest = Request.Builder()
                .url(rawUrl)
                .header("Range", "bytes=0-${LFS_MAX_CONTENT_LENGTH_BYTES - 1}")
                .get()
                .build()

            okHttpClient.newCall(getRequest).execute().use { response ->
                val bodyStart = response.peekBody(LFS_MAX_CONTENT_LENGTH_BYTES).string()
                bodyStart.startsWith(LFS_CONTENT_PREFIX) &&
                        bodyStart.contains(LFS_OID_KEYWORD) &&
                        bodyStart.contains(LFS_SIZE_KEYWORD)
            }

        } catch (e: Exception) {
            logger.warn("Failed to detect LFS status for URL: $rawUrl", e)
            false
        }
    }

    private companion object {
        // According to Git LFS specification: https://github.com/git-lfs/git-lfs/blob/main/docs/spec.md
        private const val LFS_CONTENT_TYPE = "text/plain"
        private const val LFS_MAX_CONTENT_LENGTH_BYTES = 1024L
        private const val LFS_CONTENT_PREFIX = "version https://"
        private const val LFS_OID_KEYWORD = "oid"
        private const val LFS_SIZE_KEYWORD = "size"

        private val logger = LoggerFactory.getLogger(GithubLfsDetector::class.java)
    }
}
