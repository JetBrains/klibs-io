package io.klibs.app.filter

import jakarta.servlet.ReadListener
import jakarta.servlet.ServletInputStream
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletRequestWrapper
import java.io.BufferedReader
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.io.InputStreamReader

/**
 * A [HttpServletRequestWrapper] that caches the request body so it can be read multiple times.
 */
class CachedBodyHttpServletRequest(request: HttpServletRequest) : HttpServletRequestWrapper(request) {

    private val cachedBody: ByteArray = request.inputStream.readAllBytes()

    override fun getInputStream(): ServletInputStream {
        return CachedBodyServletInputStream(cachedBody)
    }

    override fun getReader(): BufferedReader {
        return BufferedReader(InputStreamReader(ByteArrayInputStream(cachedBody)))
    }

    fun getCachedBody(): ByteArray = cachedBody

    private class CachedBodyServletInputStream(cachedBody: ByteArray) : ServletInputStream() {
        private val inputStream: InputStream = ByteArrayInputStream(cachedBody)

        override fun read(): Int = inputStream.read()

        override fun isFinished(): Boolean = inputStream.available() == 0

        override fun isReady(): Boolean = true

        override fun setReadListener(readListener: ReadListener?) {
            // Not implemented for cached body
        }
    }
}