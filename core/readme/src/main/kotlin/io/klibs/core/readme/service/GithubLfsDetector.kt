package io.klibs.core.readme.service

import okhttp3.OkHttpClient
import okhttp3.Request
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

/**
 * Some files used in GitHub readmes, such as images, are stored with Git LFS (Large File Storage).
 * For such files using the default source path with https://raw.githubusercontent.com/ prefix
 * does not work. Such paths lead to LFS pointers (in the text format), instead of actual files.
 *
 * This service detects if a file is stored using Git LFS, so it can be properly handled later.
 */
@Service
class GithubLfsDetector(
    private val okHttpClient: OkHttpClient
) {


    /**
     * Determines if the file referenced by the given URL is stored using Git LFS.
     *
     * It sends HTTP requests to evaluate if the file's format matches the LFS pointer structure
     *
     * @param rawUrl The URL of the raw file to be checked.
     * @return `true` if the file is identified as a Git LFS pointer file, `false` otherwise.
     */
    fun isLfsFile(rawUrl: String): Boolean {
        return try {

            // Send a HEAD request to check file characteristics
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

            // If the initial HEAD request was successful, send a GET request to check the content
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
