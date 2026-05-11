package io.klibs.core.readme

import io.klibs.core.readme.service.GithubLfsDetector
import okhttp3.Call
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.io.IOException

class GithubLfsDetectorTest {

    private val okHttpClient: OkHttpClient = mock()
    private val url = "https://example.com/file.png"

    @Test
    fun `isLfsFile returns true if both HEAD and GET calls confirm it's LFS`() {

        val headResponse = response(
            request = headRequest(),
            code = 200,
            headers = mapOf(
                "Content-Type" to "text/plain; charset=utf-8",
                "Content-Length" to "120"
            )
        )
        val getResponse = response(
            request = getRequest(),
            code = 206,
            body = lfsPointerBody()
        )

        whenever(okHttpClient.newCall(any())).thenAnswer { invocation ->
            val request = invocation.getArgument<Request>(0)
            when (request.method) {
                "HEAD" -> mockCallReturning(headResponse)
                "GET" -> {
                    assertEquals("bytes=0-1023", request.header("Range"))
                    mockCallReturning(getResponse)
                }
                else -> error("Unexpected request method: ${request.method}")
            }
        }

        val result = GithubLfsDetector(okHttpClient).isLfsFile(url)

        assertTrue(result)
    }

    @Test
    fun `isLfsFile returns true if both HEAD and GET calls confirm it's LFS and server does not support Range`() {
        val headResponse = response(
            request = headRequest(),
            code = 200,
            headers = mapOf(
                "Content-Type" to "text/plain",
                "Content-Length" to "120"
            )
        )
        val getResponse = response(
            request = getRequest(),
            code = 200,
            body = lfsPointerBody()
        )

        whenever(okHttpClient.newCall(any())).thenAnswer { invocation ->
            val request = invocation.getArgument<Request>(0)
            when (request.method) {
                "HEAD" -> mockCallReturning(headResponse)
                "GET" -> mockCallReturning(getResponse)
                else -> error("Unexpected request method: ${request.method}")
            }
        }

        val result = GithubLfsDetector(okHttpClient).isLfsFile(url)

        assertTrue(result)
    }

    @Test
    fun `isLfsFile returns false when HEAD content type is not text plain`() {
        val headResponse = response(
            request = headRequest(),
            code = 200,
            headers = mapOf(
                "Content-Type" to "text/javascript",
                "Content-Length" to "120"
            )
        )

        whenever(okHttpClient.newCall(any())).thenAnswer { invocation ->
            val request = invocation.getArgument<Request>(0)
            when (request.method) {
                "HEAD" -> mockCallReturning(headResponse)
                "GET" -> error("GET should not be executed when HEAD content type mismatches")
                else -> error("Unexpected request method: ${request.method}")
            }
        }

        val result = GithubLfsDetector(okHttpClient).isLfsFile(url)

        assertFalse(result)
    }

    @Test
    fun `isLfsFile returns false when HEAD content length is greater than limit`() {
        val headResponse = response(
            request = headRequest(),
            code = 200,
            headers = mapOf(
                "Content-Type" to "text/plain",
                "Content-Length" to "2048"
            )
        )

        whenever(okHttpClient.newCall(any())).thenAnswer { invocation ->
            val request = invocation.getArgument<Request>(0)
            when (request.method) {
                "HEAD" -> mockCallReturning(headResponse)
                "GET" -> error("GET should not be executed when HEAD content length exceeds limit")
                else -> error("Unexpected request method: ${request.method}")
            }
        }

        val result = GithubLfsDetector(okHttpClient).isLfsFile(url)

        assertFalse(result)
    }

    @Test
    fun `isLfsFile returns true when HEAD content type header is missing but GET call confirm it's LFS`() {
        val headResponse = response(
            request = headRequest(),
            code = 200,
            headers = mapOf("Content-Length" to "120")
        )
        val getResponse = response(
            request = getRequest(),
            code = 206,
            body = lfsPointerBody()
        )

        whenever(okHttpClient.newCall(any())).thenAnswer { invocation ->
            val request = invocation.getArgument<Request>(0)
            when (request.method) {
                "HEAD" -> mockCallReturning(headResponse)
                "GET" -> mockCallReturning(getResponse)
                else -> error("Unexpected request method: ${request.method}")
            }
        }

        val result = GithubLfsDetector(okHttpClient).isLfsFile(url)

        assertTrue(result)
    }

    @Test
    fun `isLfsFile returns true when HEAD content length header is missing but GET call confirm it's LFS`() {
        val headResponse = response(
            request = headRequest(),
            code = 200,
            headers = mapOf("Content-Type" to "text/plain")
        )
        val getResponse = response(
            request = getRequest(),
            code = 206,
            body = lfsPointerBody()
        )

        whenever(okHttpClient.newCall(any())).thenAnswer { invocation ->
            val request = invocation.getArgument<Request>(0)
            when (request.method) {
                "HEAD" -> mockCallReturning(headResponse)
                "GET" -> mockCallReturning(getResponse)
                else -> error("Unexpected request method: ${request.method}")
            }
        }

        val result = GithubLfsDetector(okHttpClient).isLfsFile(url)

        assertTrue(result)
    }

    @Test
    fun `isLfsFile returns false when http call throws exception`() {
        val failedHeadCall: Call = mock()

        whenever(okHttpClient.newCall(any())).thenReturn(failedHeadCall)
        whenever(failedHeadCall.execute()).thenThrow(IOException("Network failure"))

        val result = GithubLfsDetector(okHttpClient).isLfsFile(url)

        assertFalse(result)
    }

    private fun headRequest(): Request =
        Request.Builder()
            .url(url)
            .head()
            .build()

    private fun getRequest(): Request =
        Request.Builder()
            .url(url)
            .header("Range", "bytes=0-1023")
            .get()
            .build()

    private fun mockCallReturning(response: Response): Call {
        val call: Call = mock()
        whenever(call.execute()).thenReturn(response)
        return call
    }

    private fun response(
        request: Request,
        code: Int,
        headers: Map<String, String> = emptyMap(),
        body: String = ""
    ): Response {
        val responseBuilder = Response.Builder()
            .request(request)
            .protocol(Protocol.HTTP_1_1)
            .code(code)
            .message("OK")
            .body(body.toResponseBody())

        headers.forEach { (headerName, headerValue) ->
            responseBuilder.addHeader(headerName, headerValue)
        }

        return responseBuilder.build()
    }

    private fun lfsPointerBody(): String =
        """
        version https://git-lfs.github.com/spec/v1
        oid sha256:1234567890abcdef1234567890abcdef1234567890abcdef1234567890abcdef
        size 123
        """.trimIndent()
}