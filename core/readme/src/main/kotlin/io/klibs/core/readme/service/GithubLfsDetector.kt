package io.klibs.core.readme.service

import okhttp3.OkHttpClient
import okhttp3.Request
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

/**
 * Some files used in GitHub readmes, such as images, are stored with Git LFS (Large File Storage).
 * For such files using the default source path with https://raw.githubusercontent.com/ prefix
 * leads to an LFS pointer (in the text format), instead of an actual file.
 *
 * This service detects if a file is stored using Git LFS, so it can be properly handled later.
 * It takes the URL of the raw file and checks if it's an LFS pointer by sending HTTP requests
 * and veryfying the content of the response against the LFS pointer format.
 */
@Service
class GithubLfsDetector(
    private val okHttpClient: OkHttpClient
) {


    /**
     * Determines if the file referenced by the given URL is stored using Git LFS.
     *
     * It sends HTTP requests to evaluate if the file's format matches the LFS pointer structure
     * defined in the Git LFS specification: https://github.com/git-lfs/git-lfs/blob/main/docs/spec.md
     *
     * First, the HEAD request is sent to check the file size and type. LFS pointers are small plain-text files.
     * If the URL actually points to a big binary file, doing a GET request directly would download it.
     * Checking headers first acts as a fast, cheap filter.
     *
     * If the file looks like an LFS pointer based on headers, the actual content is fetched to confirm it.
     * The content is checked against the LFS pointer format. The Range header in the GET request is used
     * to safely download only the first kilobyte of content (the pointer length should not exceed this).
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
        // Values according to Git LFS specification: https://github.com/git-lfs/git-lfs/blob/main/docs/spec.md
        private const val LFS_CONTENT_TYPE = "text/plain"
        private const val LFS_MAX_CONTENT_LENGTH_BYTES = 1024L
        private const val LFS_CONTENT_PREFIX = "version https://"
        private const val LFS_OID_KEYWORD = "oid"
        private const val LFS_SIZE_KEYWORD = "size"

        private val logger = LoggerFactory.getLogger(GithubLfsDetector::class.java)
    }
}
